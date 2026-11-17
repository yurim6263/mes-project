# OPC UA 노드 설계

> 1주차 산출물. 초안은 `../03_확정안_SMT_미니MES_4주.md` 5-2.

```
Objects/SMT_Line1/<설비ID>/
   Status          0=Idle 1=Run 2=Down 3=Alarm
   CycleTimeSec    Double
   ProducedCount   UInt32 (누적)
   AlarmCode       String
   <설비별 센서값>  예) RFL-01: Zone1Temp, Zone2Temp
   EventSeq        UInt64 (이벤트마다 +1)
   LastEvent       String(JSON)
   Method ReadEventsSince(seq)
```

이벤트 종류: `STATUS_CHANGE` · `PRODUCED` · `REEL_LOADED` · `ALARM` · `INSPECTION_RESULT`
대체안: `ReadEventsSince` 구현이 막히면 최근 N건을 배열 노드로 노출한다.

(설비별 노드 목록·타입은 작성 예정)
