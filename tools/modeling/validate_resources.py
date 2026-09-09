import json, pathlib, struct, zlib, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[2]
pack=ROOT/'resource-pack'
vanilla=ROOT/'run/cache/gradle/caches/fabric-loom/1.21.1/minecraft-client.jar'
if vanilla.is_file():
    with zipfile.ZipFile(vanilla) as archive:
        atlas=json.loads(archive.read('assets/minecraft/atlases/blocks.json'))
    assert any(s.get('source')=='item' and s.get('type') in ('directory','minecraft:directory') for s in atlas['sources'])
else:
    print('Vanilla cache unavailable: skipping runtime atlas check; checking resource paths and PNG contents.')
checked=set()
for model_file in (pack/'assets/uglobalweapon/models').rglob('*.json'):
    model=json.loads(model_file.read_text())
    for identifier in model.get('textures',{}).values():
        if not identifier.startswith('uglobalweapon:'): continue
        path=identifier.split(':',1)[1]
        assert path.startswith('item/'),f'Not in vanilla item atlas: {identifier}'
        texture=pack/'assets/uglobalweapon/textures'/f'{path}.png'
        assert texture.is_file(),f'Missing texture: {identifier}'
        data=texture.read_bytes();assert data[:8]==b'\x89PNG\r\n\x1a\n'
        offset=8;raw=b''
        while offset<len(data):
            length=struct.unpack('>I',data[offset:offset+4])[0];kind=data[offset+4:offset+8];payload=data[offset+8:offset+8+length]
            assert zlib.crc32(kind+payload)&0xffffffff==struct.unpack('>I',data[offset+8+length:offset+12+length])[0]
            if kind==b'IDAT':raw+=payload
            offset+=12+length
        assert len(zlib.decompress(raw))==16*(1+16*3)
        checked.add(identifier)
cold=json.loads((pack/'assets/uglobalweapon/models/item/rpg_idle.json').read_text())
hot=json.loads((pack/'assets/uglobalweapon/models/item/rpg_idle_heat_3.json').read_text())
hot_parts=0
for element in hot['elements']:
    heated=any(face['texture'].startswith('#heated_') for face in element['faces'].values())
    if heated:
        assert element['to'][0]<=7.5
        hot_parts+=1
    elif element['from'][1]>=4.7 and element['to'][1]<=11.2:
        assert element['from'][0]>=7.5,'Forward barrel piece was not heated'
assert hot_parts>=12
assert all('/heat/' not in value for value in cold['textures'].values())
print(f'PASS: {len(checked)} valid item PNGs; {hot_parts} forward parts heated, cold emission absent.')
