export const FACTORY_LAYOUT_AREAS = [
  {
    areaId: 'AREA_LOAD_LC',
    areaName: 'Lower Case 로딩 구역',
    overlay: { left: 5, top: 6, width: 20, height: 30 },
    defaultTone: 'env-low'
  },
  {
    areaId: 'AREA_ASSEMBLE_01',
    areaName: '조립 구역 1',
    overlay: { left: 30, top: 6, width: 25, height: 24 },
    defaultTone: 'env-low'
  },
  {
    areaId: 'AREA_ASSEMBLE_02',
    areaName: '조립 구역 2',
    overlay: { left: 30, top: 36, width: 25, height: 24 },
    defaultTone: 'env-mid'
  },
  {
    areaId: 'AREA_OUT_BSA',
    areaName: 'BSA 출고 구역',
    overlay: { left: 60, top: 6, width: 35, height: 58 },
    defaultTone: 'env-mid'
  }
]

export const ENV_SENSOR_SLOT_KEYS = ['sensor1', 'sensor2', 'sensor3', 'sensor4']
