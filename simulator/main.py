"""SMT 라인 설비 시뮬레이터 — **스텁(0일차)**.

지금은 설비 1대(PRT-01)의 Status, ProducedCount 두 노드만 노출해
"OPC UA 서버가 뜨고 접속·읽기가 된다"는 것만 확인한다.
2주차에 03 문서 5-2의 노드 구조(설비 5대, 이벤트, ReadEventsSince)로 교체한다.
"""
import asyncio

from asyncua import Server, ua

ENDPOINT = "opc.tcp://0.0.0.0:4840/"
NAMESPACE = "urn:smt-mes:line1"


async def main() -> None:
    server = Server()
    await server.init()
    server.set_endpoint(ENDPOINT)
    server.set_server_name("SMT Line1 Simulator (stub)")
    idx = await server.register_namespace(NAMESPACE)

    line = await server.nodes.objects.add_object(idx, "SMT_Line1")
    printer = await line.add_object(idx, "PRT-01")
    status = await printer.add_variable(idx, "Status", 1, ua.VariantType.Int32)  # 1 = Run
    produced = await printer.add_variable(idx, "ProducedCount", 0, ua.VariantType.UInt32)

    async with server:
        print(f"OPC UA 시뮬레이터(스텁) 시작: {ENDPOINT}", flush=True)
        count = 0
        while True:
            await asyncio.sleep(1)
            count += 1
            await produced.write_value(ua.Variant(count, ua.VariantType.UInt32))


if __name__ == "__main__":
    asyncio.run(main())
