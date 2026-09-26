/* ArcRuntimeProbe.java — 隔离 Fabric 内验证注入、生成阶段和原生保存恢复；资源绘制不接 OpenGL。 */
package regression;

import com.zarkonnen.airships.*;
import net.poosh.arc.conquest.*;
import net.fabricacs.api.rules.SharedRules;
import net.fabricacs.api.config.*;
import org.json.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

public final class ArcRuntimeProbe {
    public static int generatedLand, generatedCityLand;
    static int checks;
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; System.out.println("PASS ARC: " + label); }
    static void reject(Runnable action, String label) { try { action.run(); } catch (IllegalArgumentException expected) { check(true,label); return; } throw new AssertionError(label); }
    static Object unsafe(Class<?> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return ((sun.misc.Unsafe)field.get(null)).allocateInstance(type);
    }
    static Object field(Object object, Class<?> type, String name) throws Exception { Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object); }
    static void set(Object object, Class<?> type, String name, Object value) throws Exception { Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(object,value); }
    static Object invoke(Object object,String name,Object...args) throws Exception {
        for(Method method:object.getClass().getDeclaredMethods()) if(method.getName().contains(name)) {
            method.setAccessible(true);try{return method.invoke(object,args);}catch(InvocationTargetException ex){throw (Exception)ex.getCause();}
        }
        throw new NoSuchMethodException(name);
    }
    static void entry(Class<? extends Loadable> type,String name) throws Exception {
        Object value=unsafe(type);set(value,Loadable.class,"name",name);Loadable.map.computeIfAbsent(type,k->new HashMap<>()).put(name,value);
    }
    static JSONObject grid(String text){return new JSONObject().put("yl",2).put("xl",2).put("data",text);}
    static JSONObject mapData(){return new JSONObject().put("worldID","arc-probe").put("lang","en").put("empires",new JSONArray()).put("terrainFeatures",new JSONArray())
        .put("water",grid("1111")).put("roads2",grid("0000")).put("connections",grid("0000")).put("cityOwnership",grid("-1 -1 -1 -1 "))
        .put("height",new JSONArray().put(0).put(0).put(0).put(0));}
    static MapSize base;
    static SharedRules rules;
    static CampaignWorld newWorld(StartValues values) throws Exception {
        rules.update(values.json());
        WorldMap map=new WorldMap(mapData(),null,null);
        CampaignWorld world=new CampaignWorld(map,null,null);world.playerEmpireIndex=-1;
        new WorldGenScreen(world,null);
        // 只调用实际注入的生成准备函数，保留原生阶段对象用于后面的定向执行。
        invoke(map,"arc$prepare",new org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean>("test",false));
        return world;
    }
    static Empire empire(boolean human,int id) throws Exception {
        Empire empire=(Empire)unsafe(Empire.class);empire.playerControlled=human;set(empire,Empire.class,"cities",new ArrayList<City>());
        empire.cities.add(new City(id,35+id*25,35+id*25,"capital",false,30,null,1));empire.setMoney(777);
        return empire;
    }
    static Object stage(WorldMap map,int index) throws Exception {return ((Object[])field(map,WorldMap.class,"setupStages"))[index];}
    static boolean runStage(Object stage,int index,WorldMap map) throws Exception {return (Boolean)invoke(stage,"run",index,map);}
    static String placeAll() throws Exception {
        CampaignWorld world=newWorld(new StartValues(3,3,100));WorldMap map=world.map;
        map.water=new boolean[128][128];map.setupCityNames=new ArrayList<>(List.of("a","b","c","d","e","f"));
        map.empires.add(empire(true,0));map.empires.add(empire(false,1));map.r=new GuardedRandom(89412);
        Object stage=stage(map,2);int count=(Integer)invoke(stage,"getSize");check(count==10,"native placement stage uses expanded capacity");
        for(int i=0;i<count;i++)runStage(stage,i,map);
        check(map.empires.get(0).cities.size()==6&&map.empires.get(0).cities.stream().filter(c->!c.isTown).count()==3,"native placement creates exactly three cities and three towns for human");
        check(map.empires.get(1).cities.size()==3&&map.empires.get(1).cities.stream().filter(c->!c.isTown).count()==1,"native placement keeps AI at one city and two towns");
        Set<Integer> ids=new HashSet<>();StringBuilder result=new StringBuilder();
        for(Empire empire:map.empires)for(City city:empire.cities){check(ids.add(city.id),"unique native settlement ID "+city.id);result.append(city.id).append(':').append(city.x).append(',').append(city.y).append(',').append(city.isTown).append(',').append(city.income).append(';');}
        check(java.util.stream.IntStream.range(0,9).allMatch(id -> map.getCity(id)!=null)
            && map.getCity(9)==null,"native city cache contains contiguous IDs 0 through 8 only");
        return result.toString();
    }
    /** 在真实放置后验证领土描边；只构造小块归属网格，不冒充完整地形或 GPU 验收。 */
    static void checkTerritoryIds() throws Exception {
        StartValues[] settings = {
            new StartValues(-1,-1,-1), new StartValues(-1,-1,42), new StartValues(1,2,-1),
            new StartValues(2,8,-1), new StartValues(1,0,-1), new StartValues(4,8,-1),
            new StartValues(2,0,-1), new StartValues(-1,1,-1)
        };
        for (int empireCount : new int[]{2,4}) {
            for (int humanParity=0; humanParity<2; humanParity++) {
                String defaults = null;
                for (int i=0; i<settings.length; i++) {
                    String snapshot = placeAndTrace(empireCount,humanParity,settings[i],false);
                    if (i==0) defaults=snapshot;
                    if (i==1 || i==2) check(snapshot.equals(defaults),
                        "cash-only/explicit defaults preserve IDs, placement, types, income and RNG: "
                            + empireCount + "/" + humanParity + "/" + settings[i]);
                }
            }
        }
        // 人类首槽之后强制 AI 选址失败，随后恢复陆地；失败不应消耗连续 ID。
        placeAndTrace(2,0,new StartValues(2,8,-1),true);
    }
    static String placeAndTrace(int empireCount,int humanParity,StartValues values,boolean failAI) throws Exception {
        base=new MapSize(new JSONObject().put("name","SMALLISH").put("gridSize",8)
            .put("empires",empireCount).put("nests",0));
        Loadable.map.get(MapSize.class).put(base.name,base);
        WorldMap map=newWorld(values).map;
        int gridSize=map.size.gridSize;
        map.water=new boolean[gridSize][gridSize];
        map.setupCityNames=new ArrayList<>(List.of("a","b","c","d","e","f"));
        for (int i=0; i<empireCount; i++) map.empires.add(empire(i%2==humanParity,i));
        map.r=new GuardedRandom(89412+humanParity);
        Object placement=stage(map,2);
        int slots=(Integer)invoke(placement,"getSize");
        for (int i=0; i<slots; i++) {
            if (failAI && i==1) for (boolean[] row:map.water) Arrays.fill(row,true);
            runStage(placement,i,map);
            if (failAI && i==1) for (boolean[] row:map.water) Arrays.fill(row,false);
        }
        String label=empireCount+"/"+humanParity+"/"+values+"/failAI="+failAI;
        Set<Integer> ids=new TreeSet<>();
        ArrayList<City> settlements=new ArrayList<>();
        StringBuilder snapshot=new StringBuilder();
        for (int i=0; i<empireCount; i++) {
            Empire empire=map.empires.get(i);
            int expected=empire.playerControlled ? values.cityCount()+values.townCount(base.townsPerEmpire)
                : 1+base.townsPerEmpire;
            if (failAI && i==1) expected--;
            check(empire.cities.size()==expected,label+" settlement count for empire "+i);
            check(empire.cities.stream().filter(city -> !city.isTown).count()
                ==(empire.playerControlled ? values.cityCount() : 1),label+" city types for empire "+i);
            for (City city:empire.cities) {
                settlements.add(city);
                ids.add(city.id);
                snapshot.append(i).append(':').append(city.id).append(':').append(city.x).append(',')
                    .append(city.y).append(',').append(city.isTown).append(',').append(city.income).append(';');
            }
        }
        check(ids.size()==settlements.size(),label+" unique IDs");
        check(java.util.stream.IntStream.range(0,settlements.size()).allMatch(ids::contains),label+" contiguous IDs");
        check(settlements.stream().allMatch(city -> map.getCity(city.id)==city)
            && map.getCity(settlements.size())==null,label+" city cache matches settlement identities");
        // 保留真实放置得到的 ID，把测试领土排列为隔开的单格，避免高数量场景夹具重叠。
        // 这里不模拟影响力分配；直接调用游戏字节码，检查 ID 空洞是否截断领土描边。
        int[][] ownership=new int[3][settlements.size()*3+2];
        for (int[] row:ownership) Arrays.fill(row,-1);
        for (int i=0; i<settlements.size(); i++) ownership[1][i*3+1]=settlements.get(i).id;
        ArrayList<ShapeUtils.Area> areas=new ArrayList<>();
        ShapeUtils.cityOwnershipAreas(ownership,new HashMap<>(),new ArrayList<>(),areas);
        Set<Integer> tracedIds=new HashSet<>();
        for (ShapeUtils.Area area:areas) tracedIds.add(area.identifier);
        check(areas.size()==settlements.size() && tracedIds.equals(ids),label+" native territory area for every settlement");
        return snapshot.append("rng=").append(map.r.nextInt()).toString();
    }
    public static void main(String[] args) throws Exception {
        for(String name:List.of("WorldMap","WorldMap$3","WorldMap$4","GameSetupScreen","CampaignWorld","WorldGenScreen")) {
            Class<?> type=Class.forName("com.zarkonnen.airships."+name);
            check(Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("arc$")||m.getName().contains("acbric$")),"real mixin transformation: "+name);
        }
        rules=(SharedRules)field(null,StartingOptions.class,"rules");check(rules!=null,"real ARC initializer declared rules");
        Lang.currentLocale=Locale.forLanguageTag("chi");check(StartingOptions.title().contains("玩家"),"Chinese follows game");
        Lang.currentLocale=Locale.ENGLISH;check(StartingOptions.title().contains("Player"),"English follows game");
        check(StartingOptions.window()!=null,"settings window builds using real config editor");
        reject(()->new StartValues(0,0,0),"zero cities rejected");reject(()->new StartValues(5,0,0),"cities upper bound");
        reject(()->new StartValues(1,9,0),"town upper bound");reject(()->new StartValues(1,0,-2),"cash lower bound");
        reject(()->StartValues.read(new JSONObject().put("cities","2").put("towns",0).put("cash",0)),"string values rejected");
        Loadable.map=new HashMap<>();Loadable.alls=null;
        entry(DifficultyLevel.class,"NORMAL");entry(MonsterSetting.class,"DEFAULT");entry(SeaLevelSetting.class,"MIXED");
        entry(FrequencySetting.class,"DEFAULT");entry(TechSpeedSetting.class,"NORMAL");entry(EraModifier.class,"NO_BONUS");entry(StrategicEra.class,"INITIAL");
        entry(HeraldicStyle.class,"city");
        Loadable.map.put(LoadingQuote.class,new HashMap<>());
        base=new MapSize(new JSONObject().put("name","SMALLISH").put("gridSize",4).put("empires",2).put("nests",0));
        Loadable.map.computeIfAbsent(MapSize.class,k->new HashMap<>()).put(base.name,base);
        CampaignWorld vanilla=newWorld(new StartValues(-1,-1,-1));
        check(vanilla.map.size==base,"defaults preserve global size identity");
        CampaignWorld cashOnly=newWorld(new StartValues(-1,-1,0));check(cashOnly.map.size==base,"cash only does not alter map");
        CampaignWorld world=newWorld(new StartValues(3,3,12345));WorldMap map=world.map;
        check(map.size!=base&&map.size.townsPerEmpire==5&&base.townsPerEmpire==2,"per-map capacity expanded, global unchanged");
        check(map.size.gridSize==base.gridSize&&map.size.empires==base.empires,"geometry and empire count unchanged");
        rules.update(new StartValues(1,0,0).json());check(StartingOptions.forMap(map).equals(new StartValues(3,3,12345)),"candidate change cannot change frozen map rules");
        check(newWorld(new StartValues(-1,-1,-1)).map.size==base,"next vanilla campaign not polluted");
        reject(()->MapLayout.read(base,new JSONObject().put(MapLayout.KEY,new JSONObject().put("version",2).put("slots",5))),"unknown layout version rejected");
        reject(()->MapLayout.read(base,new JSONObject().put(MapLayout.KEY,new JSONObject().put("version",1).put("slots",500))),"invalid layout capacity rejected");
        SavedStateOutPipe initialState=new SavedStateOutPipe();JSONObject saved=map.toJSON(initialState);initialState.compileAndGetHash();
        check(saved.getJSONObject(MapLayout.KEY).getInt("slots")==5,"real native serializer includes capacity");
        WorldMap read=new WorldMap(saved,null,new JSONObjectInPipe(initialState.toJSON()));check(read.size.townsPerEmpire==5&&base.townsPerEmpire==2,"real native JSON constructor restores capacity without global mutation");
        check(((GenerationAccess)read).arc$options()==null,"load does not arm generation logic");
        SavedStateOutPipe state=new SavedStateOutPipe();JSONObject stateMap=read.toJSON(state);state.compileAndGetHash();
        WorldMap restored=new WorldMap(stateMap,null,new JSONObjectInPipe(state.toJSON()));
        check(restored.size.townsPerEmpire==5,"native binary state reconstruction retains capacity");
        check(StartingOptions.forMap(restored).equals(new StartValues(3,3,12345)),"native reconstruction retains shared rules");
        Path saveDir=Files.createDirectories(AGame.getGameDirectory().toPath().resolve("saves")).resolve("arc-test.json");
        IODirectory output=new IODirectory(saveDir.toFile(),map.worldID);JSONObject worldJson=world.toJSON(output);
        output.registerWithoutVersion(id->worldJson,"world");output.write();
        var input=OpenGameMission.load(saveDir.toFile());CampaignWorld loaded=new CampaignWorld(input.a,null,true,input.b);
        check(loaded.map.size.townsPerEmpire==5,"native disk save/load retains layout");
        check(StartingOptions.forMap(loaded.map).equals(new StartValues(3,3,12345)),"native disk save/load uses saved values");
        map.empires.add(empire(true,0));map.empires.add(empire(false,1));
        Object townStage=stage(map,2);map.r=new GuardedRandom(123);
        check(runStage(townStage,5,map),"AI expanded slot skipped by actual native stage");
        check(map.empires.get(1).cities.size()==1&&map.r.nextInt()==new GuardedRandom(123).nextInt(),"skipped AI slot does not mutate RNG or cities");
        City extra=new City(2,40,40,"extra",true,1,null,0);
        set(map.difficulty,DifficultyLevel.class,"playerIncome",33);
        int income=(Integer)invoke(townStage,"arc$settlementIncome",map,true,extra,map.r,0,map);
        check(!extra.isTown&&income==33,"extra city typed before land generation and uses native player income");
        City town=new City(6,50,50,"town",true,1,null,0);
        income=(Integer)invoke(townStage,"arc$settlementIncome",map,true,town,map.r,4,map);
        check(town.isTown&&income>=5&&income<=17,"remaining player slots keep native town income");
        City aiTown=new City(3,60,60,"AI",true,1,null,0);
        invoke(townStage,"arc$settlementIncome",map,false,aiTown,map.r,1,map);check(aiTown.isTown,"AI town never upgraded");
        Object rituals=stage(map,3);check((Integer)invoke(rituals,"arc$boundedRituals",2,1,0,map)==0,"zero towns has zero ritual sites");
        map.empires.get(1).cities.add(aiTown);check((Integer)invoke(rituals,"arc$boundedRituals",2,1,0,map)==1,"ritual count capped to available towns");
        // 全水地图确保原生选址失败；应在首次保存前给出可操作错误，不能假装达到设置数量。
        map.water=new boolean[128][128];for(boolean[] row:map.water)Arrays.fill(row,true);
        try{runStage(townStage,0,map);throw new AssertionError("missing placement accepted");}
        catch(IllegalStateException expected){check(expected.getMessage().contains("ARC"),"native placement failure produces explicit ARC error");}
        Empire human=map.empires.get(0);human.cities.add(extra);human.cities.add(new City(4,1,1,"city",false,30,null,0));
        human.cities.add(town);human.cities.add(new City(8,1,1,"town2",true,8,null,0));human.cities.add(new City(10,1,1,"town3",true,8,null,0));
        map.campaignWorldDuringGen=world;world.setupPlayer();
        check(human.getMoney()==12345&&map.empires.get(1).getMoney()==777,"actual CREATED hook sets human cash only");
        human.setMoney(2);world.setupPlayer();check(human.getMoney()==2,"second setupPlayer cannot regrant cash");
        map.campaignWorldDuringGen=null;
        CampaignWorld zero=newWorld(new StartValues(1,0,0));zero.map.empires.add(empire(true,0));zero.map.empires.add(empire(false,1));
        check(runStage(stage(zero.map,2),0,zero.map),"zero player towns skips first slot");
        zero.map.campaignWorldDuringGen=zero;zero.setupPlayer();check(zero.map.empires.get(0).getMoney()==0,"zero cash is valid");
        ModConfig config=(ModConfig)field(null,StartingOptions.class,"config");
        config.save(config.read(),new StartValues(2,1,456).json());config.reload();check(StartValues.read(config.read().data()).equals(new StartValues(2,1,456)),"real config persistence");
        check(placeAll().equals(placeAll()),"same seed and settings produce identical native placements with isolated asset fixtures");
        check(generatedLand==14&&generatedCityLand==4,"actual native placement reaches land hook with correct city types");
        checkTerritoryIds();
        System.out.println("ARC RUNTIME PASS: "+checks+" checks");
    }
}
