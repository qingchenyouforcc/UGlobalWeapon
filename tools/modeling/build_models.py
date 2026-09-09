import base64, copy, json, pathlib, struct, uuid, zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'resource-pack/assets/uglobalweapon'
MODELS = ASSETS / 'models/item'
PALETTE = {'olive': '#586b31', 'light': '#81964a', 'dark': '#242c26', 'metal': '#465049',
           'yellow': '#d1ae39', 'rubber': '#161c19', 'silver': '#919d88', 'lens': '#6db8a2',
           'warm': '#754235', 'hot': '#b24a2e', 'red': '#d7442e'}

def png(color):
    rgb = bytes.fromhex(color[1:])
    raw = b''.join(b'\0' + rgb * 16 for _ in range(16))
    def chunk(t, data):
        return struct.pack('>I', len(data)) + t + data + struct.pack('>I', zlib.crc32(t + data) & 0xffffffff)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB',16,16,8,2,0,0,0)) + chunk(b'IDAT',zlib.compress(raw)) + chunk(b'IEND',b'')

def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')

def cube(name, a, b, texture):
    return {'name':name, 'from':a, 'to':b, 'faces':{side:{'texture':'#'+texture, 'uv':[0,0,16,16]} for side in ['north','south','east','west','up','down']}}

gun=[]
def g(name,a,b,t): gun.append(cube(name,a,b,t))
# Hollow launch tube: the black opening is geometry, not a painted solid end cap.
g('upper barrel',[-2,9.4,6],[17,10.2,10],'olive')
g('lower barrel',[-2,5.8,6],[17,6.6,10],'olive')
g('near barrel',[-2,6.6,5.8],[17,9.4,6.6],'olive')
g('far barrel',[-2,6.6,9.4],[17,9.4,10.2],'olive')
for x in [-2,16]:
    g('reinforced ring top',[x,10.2,5],[x+1.4,11.2,11],'dark')
    g('reinforced ring bottom',[x,4.8,5],[x+1.4,5.8,11],'dark')
    g('reinforced ring near',[x,5.8,4.8],[x+1.4,10.2,5.8],'dark')
    g('reinforced ring far',[x,5.8,10.2],[x+1.4,10.2,11.2],'dark')
# Only the forward sleeve uses the heat texture.
g('hot sleeve top',[-0.5,10.2,6],[3.5,10.6,10],'heat')
g('hot sleeve near',[-0.5,6,5.4],[3.5,10,5.8],'heat')
g('hot sleeve far',[-0.5,6,10.2],[3.5,10,10.6],'heat')
g('hot sleeve bottom',[-0.5,5.4,6],[3.5,5.8,10],'heat')
g('top highlight',[3.5,10.2,6.4],[15.8,10.45,7],'light')
g('side armor',[4,6.5,5.45],[15.8,9.3,5.8],'metal')
g('side olive stripe',[4.2,8.5,5.2],[15.5,9.1,5.45],'olive')
for x in [4.8,6,7.2]: g('identification stripe',[x,7.2,5.1],[x+0.6,8.2,5.45],'yellow')
for x in [9,10.2,11.4,12.6]: g('cooling slot',[x,8.7,5.05],[x+0.5,9.15,5.4],'rubber')
g('front handle mount',[7,10.2,7],[8,12.4,9],'dark')
g('rear handle mount',[12,10.2,7],[13,12.4,9],'dark')
g('carry handle',[7,12.4,7],[13,13.4,9],'dark')
g('handle highlight',[7.4,12.5,6.8],[12.6,13.1,7],'olive')
g('rear sight',[13.5,10.2,7],[15.2,11.7,9],'metal')
g('optic lens',[13.4,10.6,7.3],[13.5,11.35,8.7],'lens')
g('trigger receiver',[10,4.7,6.3],[15,5.8,9.7],'metal')
g('pistol grip',[12,1.3,7],[14,4.7,9],'rubber')
g('grip panel',[11.8,1.8,7.2],[12,4.2,8.8],'olive')
g('trigger guard',[9.8,2.9,7.2],[10.3,4.7,8.8],'dark')
g('trigger guard base',[9.8,2.5,7.2],[12.2,3,8.8],'dark')
g('trigger',[11,3.5,7.7],[11.4,4.7,8.3],'silver')
g('shoulder pad',[16,3.6,6],[18,4.8,10],'rubber')
for x in [4.1,14.8]: g('captive screw',[x,6.7,5.1],[x+0.5,7.2,5.45],'silver')

rocket=[]
def r(name,a,b,t): rocket.append(cube(name,a,b,t))
r('motor case',[6.6,6.6,1.5],[9.4,9.4,11],'olive')
r('warhead shoulder',[6.1,6.1,10.5],[9.9,9.9,13.2],'olive')
r('warhead taper',[6.6,6.6,13.2],[9.4,9.4,15],'light')
r('warhead nose',[7.2,7.2,15],[8.8,8.8,16.5],'olive')
r('nose cap',[7.6,7.6,16.5],[8.4,8.4,17.3],'dark')
r('yellow band',[6.45,6.45,9.3],[9.55,9.55,10.3],'yellow')
r('rear collar',[6.25,6.25,0.5],[9.75,9.75,1.8],'metal')
r('nozzle',[7,7,-0.3],[9,9,0.5],'rubber')
r('exhaust core',[7.6,7.6,-0.5],[8.4,8.4,-0.3],'dark')
for a,b in [([4.4,7.75,1.8],[6.6,8.25,5.3]),([9.4,7.75,1.8],[11.6,8.25,5.3]),
            ([7.75,4.4,1.8],[8.25,6.6,5.3]),([7.75,9.4,1.8],[8.25,11.6,5.3])]: r('stabilizer fin',a,b,'metal')
r('service marking',[6.4,7,6.2],[6.6,8.7,7],'silver')

def heat_color(color,tier,strength=1):
    original=[int(color[i:i+2],16) for i in (1,3,5)]
    target=[int(PALETTE['red'][i:i+2],16) for i in (1,3,5)]
    blend=[0,.45,.68,.86][tier]*strength
    return '#'+''.join(f'{round(a*(1-blend)+b*blend):02x}' for a,b in zip(original,target))

# Split the entire forward barrel, including its opening ring and outer armor.
# Accessories above the tube and the rear half remain at normal temperature.
split=[]
strengths={}
for element in gun:
    part=copy.deepcopy(element)
    for face in part['faces'].values():
        if face['texture']=='#heat': face['texture']='#metal'
    if part['from'][1]>=4.7 and part['to'][1]<=11.2 and part['from'][0]<7.5:
        end=min(part['to'][0],7.5)
        start=part['from'][0]
        while start < end-0.001:
            front=copy.deepcopy(part);front['from'][0]=start;front['to'][0]=min(end,start+.5)
            front['name']+=' (graded heat zone)'
            strength=max(.04,1-((start+front['to'][0])/2+2)/9.5)
            for face in front['faces'].values():
                base=face['texture'][1:]; key=base+'_band_'+str(round(strength*100))
                PALETTE[key]=PALETTE[base];strengths[key]=strength
                face['texture']='#heated_'+key
            split.append(front);start=front['to'][0]
        if part['to'][0]>7.5:
            part['from'][0]=7.5;split.append(part)
    else: split.append(part)
gun=split

def model(elements, display, tier=0):
    textures={key:'uglobalweapon:item/palette/'+key for key in PALETTE}
    textures.update({'heated_'+key:('uglobalweapon:item/palette/'+key if tier==0 else f'uglobalweapon:item/heat/{tier}/'+key) for key in PALETTE})
    textures.update(particle='uglobalweapon:item/palette/olive')
    return {'gui_light':'side','textures':textures,'display':display,'elements':elements}

idle=json.loads((MODELS/'rpg_idle.json').read_text(encoding='utf-8'))
display=idle['display']
display['gui']={'rotation':[20,210,0],'translation':[0,0,0],'scale':[0.75]*3}
frames={1001:'rpg_idle',**{1002+i:f'rpg_raise_{i+1}' for i in range(5)},**{1020+i:f'rpg_recoil_{i+1}' for i in range(4)}}
baseframes={n:json.loads((MODELS/(name+'.json')).read_text()) for n,name in frames.items() if n!=1001}
for tier,heat in enumerate(['metal','warm','hot','red']):
    suffix='' if tier==0 else f'_heat_{tier}'
    save(MODELS/f'rpg_idle{suffix}.json',model(gun,display,tier))
    for n,frame in baseframes.items():
        updated=copy.deepcopy(frame); updated['parent']='uglobalweapon:item/rpg_idle'+suffix
        save(MODELS/(frames[n]+suffix+'.json'),updated)
star=json.loads((ROOT/'resource-pack/assets/minecraft/models/item/nether_star.json').read_text())
star['overrides']=[{'predicate':{'custom_model_data':n+tier*100},'model':'uglobalweapon:item/'+name+('' if tier==0 else f'_heat_{tier}')} for tier in range(4) for n,name in sorted(frames.items())]
save(ROOT/'resource-pack/assets/minecraft/models/item/nether_star.json',star)
rocketdisplay={'gui':{'rotation':[25,135,0],'scale':[0.85]*3},'ground':{'translation':[0,2,0],'scale':[0.65]*3},
               'firstperson_righthand':{'rotation':[0,0,-15],'scale':[0.75]*3},'firstperson_lefthand':{'rotation':[0,0,15],'scale':[0.75]*3}}
save(MODELS/'rocket_round.json',model(rocket,rocketdisplay))
save(ROOT/'resource-pack/assets/minecraft/models/item/prismarine_shard.json',{'parent':'item/generated','textures':{'layer0':'item/prismarine_shard'},'overrides':[{'predicate':{'custom_model_data':2001},'model':'uglobalweapon:item/rocket_round'}]})
for key,color in PALETTE.items():
    path=ASSETS/f'textures/item/palette/{key}.png'; path.parent.mkdir(parents=True,exist_ok=True); path.write_bytes(png(color))
    for tier in [1,2,3]:
        path=ASSETS/f'textures/item/heat/{tier}/{key}.png';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(png(heat_color(color,tier,strengths.get(key,1))))

def blockbench(name,elements,display,hot=False):
    colors=dict(PALETTE)
    if hot: colors.update({'heated_'+k:heat_color(c,3,strengths.get(k,1)) for k,c in PALETTE.items()})
    keys=list(colors)
    textures=[{'name':k+'.png','id':str(i),'uuid':str(uuid.uuid4()),'width':16,'height':16,'source':'data:image/png;base64,'+base64.b64encode(png(colors[k])).decode()} for i,k in enumerate(keys)]
    cubes=[]
    for element in elements:
        e=copy.deepcopy(element); e.update(type='cube',uuid=str(uuid.uuid4()),box_uv=False,autouv=0)
        for face in e['faces'].values():
            key=face['texture'][1:]; face['texture']=keys.index(key if hot else key.removeprefix('heated_'))
        cubes.append(e)
    save(ROOT/'models'/f'{name}.bbmodel',{'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':name,'resolution':{'width':16,'height':16},'elements':cubes,'outliner':[e['uuid'] for e in cubes],'textures':textures,'display':display})

blockbench('olive_launcher',gun,display)
blockbench('olive_launcher_hot',gun,display,True)
blockbench('rocket_round',rocket,rocketdisplay)
save(ROOT/'models/model_geometry.json',{'palette':PALETTE|{'heated_'+k:heat_color(c,3,strengths.get(k,1)) for k,c in PALETTE.items()},'launcher':gun,'rocket':rocket})
print(f'Built {len(gun)}-part launcher, {len(rocket)}-part rocket, 40 heat/pose models and editable Blockbench files.')
