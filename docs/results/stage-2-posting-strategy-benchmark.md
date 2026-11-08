# Stage 2 benchmark: the three overdraft-prevention strategies

Saved output of the CI benchmark job. Every number in ADR-0014 comes from this file.

- Run: https://github.com/shreyescodes/ledger-line/actions/runs/42469243860 (job "Posting strategy benchmark")
- Branch head: `f9bda87` on `stage-2-ledger-core` (the runner tests the PR merge commit shown below)
- Command: `./gradlew :services:ledger-service:benchmark` (`PostingStrategyBenchmark`)
- Postgres ran in a Testcontainers container on the same 4-vCPU runner as the JVM, with default settings. Treat the numbers as relative, not as production capacity.

## Machine (machine.txt)

```text
runner: GitHub Actions 1000003518 (ubuntu24 20261101.360.1)
commit: 226064dd986e43361ac1696983d7b8875716f17d
CPU(s):                                  4
Model name:                              AMD EPYC 9V74 80-Core Processor
Thread(s) per core:                      2
Core(s) per socket:                      2
               total        used        free      shared  buff/cache   available
Mem:           15983        1598       12642          47        2096       14385
docker: 28.0.4
```

## Posting strategy benchmark (raw output)

- Java: Eclipse Adoptium 25.0.5
- Processors visible to the JVM: 4
- Postgres: PostgreSQL 18.6 on x86_64-pc-linux-musl, compiled by gcc (Alpine 15.2.0) 15.2.0, 64-bit
- Workers: 16 threads, connection pool 16; 4000 transfers per run, 3 runs per cell after a 500-transfer warm-up

Median of the runs in each cell:

| Strategy | Scenario | Throughput (transfers/s) | p50 ms | p95 ms | p99 ms | max ms | Retries | Gave up |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| ORDERED_LOCKS | HOT | 1351 | 6.05 | 44.89 | 80.67 | 192.92 | 0 | 0 |
| ORDERED_LOCKS | SPREAD | 2143 | 7.31 | 11.63 | 14.58 | 23.90 | 0 | 0 |
| OPTIMISTIC | HOT | 888 | 4.88 | 101.35 | 175.32 | 231.01 | 6409 | 81 |
| OPTIMISTIC | SPREAD | 1727 | 8.78 | 15.16 | 21.19 | 36.08 | 197 | 0 |
| SERIALIZABLE | HOT | 699 | 8.28 | 117.89 | 183.55 | 254.58 | 7378 | 96 |
| SERIALIZABLE | SPREAD | 1177 | 9.79 | 35.47 | 58.43 | 151.74 | 1676 | 0 |

## Every run (posting-strategies.jsonl)

```json
{"strategy":"ORDERED_LOCKS","scenario":"HOT","repetition":1,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":0,"seconds":3.309,"throughput":1209.0,"p50Ms":6.566,"p95Ms":52.297,"p99Ms":100.492,"maxMs":192.918}
{"strategy":"ORDERED_LOCKS","scenario":"HOT","repetition":2,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":0,"seconds":2.962,"throughput":1350.5,"p50Ms":6.051,"p95Ms":44.889,"p99Ms":80.667,"maxMs":214.497}
{"strategy":"ORDERED_LOCKS","scenario":"HOT","repetition":3,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":0,"seconds":2.666,"throughput":1500.2,"p50Ms":5.261,"p95Ms":42.299,"p99Ms":74.755,"maxMs":176.293}
{"strategy":"ORDERED_LOCKS","scenario":"SPREAD","repetition":1,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":0,"seconds":1.854,"throughput":2157.9,"p50Ms":7.182,"p95Ms":11.634,"p99Ms":14.583,"maxMs":23.901}
{"strategy":"ORDERED_LOCKS","scenario":"SPREAD","repetition":2,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":0,"seconds":1.892,"throughput":2114.5,"p50Ms":7.343,"p95Ms":12.187,"p99Ms":15.385,"maxMs":27.381}
{"strategy":"ORDERED_LOCKS","scenario":"SPREAD","repetition":3,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":0,"seconds":1.866,"throughput":2143.4,"p50Ms":7.311,"p95Ms":11.383,"p99Ms":14.320,"maxMs":18.859}
{"strategy":"OPTIMISTIC","scenario":"HOT","repetition":1,"transfers":4000,"posted":3925,"rejected":0,"gaveUp":75,"retries":6385,"seconds":4.235,"throughput":944.5,"p50Ms":4.879,"p95Ms":93.555,"p99Ms":171.384,"maxMs":231.007}
{"strategy":"OPTIMISTIC","scenario":"HOT","repetition":2,"transfers":4000,"posted":3919,"rejected":0,"gaveUp":81,"retries":6409,"seconds":4.502,"throughput":888.4,"p50Ms":4.805,"p95Ms":104.173,"p99Ms":175.316,"maxMs":220.746}
{"strategy":"OPTIMISTIC","scenario":"HOT","repetition":3,"transfers":4000,"posted":3911,"rejected":0,"gaveUp":89,"retries":6669,"seconds":4.650,"throughput":860.2,"p50Ms":5.615,"p95Ms":101.349,"p99Ms":177.035,"maxMs":250.511}
{"strategy":"OPTIMISTIC","scenario":"SPREAD","repetition":1,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":197,"seconds":2.309,"throughput":1732.4,"p50Ms":8.687,"p95Ms":15.164,"p99Ms":21.193,"maxMs":36.514}
{"strategy":"OPTIMISTIC","scenario":"SPREAD","repetition":2,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":200,"seconds":2.326,"throughput":1719.7,"p50Ms":8.777,"p95Ms":15.641,"p99Ms":21.352,"maxMs":32.584}
{"strategy":"OPTIMISTIC","scenario":"SPREAD","repetition":3,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":195,"seconds":2.316,"throughput":1726.8,"p50Ms":8.906,"p95Ms":15.123,"p99Ms":20.136,"maxMs":36.079}
{"strategy":"SERIALIZABLE","scenario":"HOT","repetition":1,"transfers":4000,"posted":3904,"rejected":0,"gaveUp":96,"retries":7326,"seconds":5.794,"throughput":690.4,"p50Ms":8.283,"p95Ms":120.708,"p99Ms":189.289,"maxMs":249.658}
{"strategy":"SERIALIZABLE","scenario":"HOT","repetition":2,"transfers":4000,"posted":3905,"rejected":0,"gaveUp":95,"retries":7444,"seconds":5.724,"throughput":698.8,"p50Ms":8.284,"p95Ms":117.894,"p99Ms":183.298,"maxMs":260.213}
{"strategy":"SERIALIZABLE","scenario":"HOT","repetition":3,"transfers":4000,"posted":3903,"rejected":0,"gaveUp":97,"retries":7378,"seconds":5.667,"throughput":705.8,"p50Ms":8.115,"p95Ms":114.879,"p99Ms":183.555,"maxMs":254.581}
{"strategy":"SERIALIZABLE","scenario":"SPREAD","repetition":1,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":1676,"seconds":3.447,"throughput":1160.3,"p50Ms":9.786,"p95Ms":35.509,"p99Ms":62.769,"maxMs":151.741}
{"strategy":"SERIALIZABLE","scenario":"SPREAD","repetition":2,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":1684,"seconds":3.399,"throughput":1176.9,"p50Ms":9.883,"p95Ms":35.474,"p99Ms":58.433,"maxMs":168.072}
{"strategy":"SERIALIZABLE","scenario":"SPREAD","repetition":3,"transfers":4000,"posted":4000,"rejected":0,"gaveUp":0,"retries":1463,"seconds":3.257,"throughput":1228.2,"p50Ms":9.664,"p95Ms":32.696,"p99Ms":52.904,"maxMs":140.802}
```
