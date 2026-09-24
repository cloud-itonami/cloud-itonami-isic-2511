# physai-isic-2511 — 構造用金属製品製造業（鉄骨製作） の physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isic-2511`、ISIC 2511 構造用金属製品製造業）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: ロボットが溶接・組立（仮付け）・非破壊検査の走査を行い、actor が提案し独立した Structural Fabrication Governor が gate する（組立品の出荷と製作証明書の発行は人の承認が要る）。
その物理的な仕事を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:flange-preheat` | thermal | 誘導加熱パッドで柱フランジの片面を 250 °C に保ち、反対面（静止空気、工場 20 °C）が予熱 100 °C に達するまで。溶接ロボットはそれまでアークを出さない | 反対面 100 °C 到達時間 | 600 s（estimate） |
| `:stiffener-fit-up` | manipulator | 組立アームがスチフナをキット棚から持ち上げ、仮付けのため桁ウェブに当てる（2 リンクアーム） | 肩関節ピークトルク | 800 N·m（estimate） |
| `:plate-lot-coupon` | material | 受入鋼板ロットから切り出した 12 × 25 mm 試験片（標点 100 mm）を 110 kN まで引張り、0.2 % オフセット降伏荷重を判定。sweep はロットの降伏応力 | 0.2 % オフセット降伏荷重 | 73500 N 以上（JIS G 3101 SS400、出典あり） |

測定の入口: `kbb -M:dev:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:dev:physai-test`（`test-physai/structuralsteel/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する）。


## 測って分かったこと・限界（成長の第一候補）

1. **予熱**: 板厚 20 mm で 9.0 s、40 mm で 36.6 s、60 mm で 82.3 s、100 mm で 228.6 s（厚さの 2 乗に比例）。10 分に収まる板厚は **162 mm** まで —— 加熱面を 250 °C に固定しているので速い。実際は加熱パッドの出力が有限で面温度が上がるまでに時間がかかる（固定温度の仮定が最初の見直し対象）。
2. **スチフナ組立**: 肩トルクは 10 kg で 268.8 N·m、30 kg で 474.9 N·m、60 kg で 785.8 N·m。800 N·m に達するのは **61.4 kg**。
3. **受入試験片**: 0.2 % オフセット降伏荷重は降伏応力 235 MPa で 73.15 kN（不合格）、245 MPa で 75.90 kN、290 MPa で 89.65 kN（公称値より約 3.1〜3.8 % 高い）。判定が反転する降伏応力は **236.6 MPa** —— 実の降伏点 237〜245 MPa の鋼板を solver は SS400 合格と判定する。加工硬化とフレーム刻みでオフセット降伏が高めに出る solver の限界で、境界付近の合否はこの数値だけで決めない。弾性剛性 6.15×10⁸ N/m は理論値 EA/L と一致。
4. **estimate のままの値**（成長候補）: 予熱 10 分の枠と予熱温度 100 °C（溶接施工要領書 / JIS Z 3700 や AWS D1.1 の予熱表で板厚・炭素当量から置き換える）、加熱面温度と空気側熱伝達係数、肩トルク 800 N·m（アームの仕様書）、加工硬化係数 1 GPa。限界 73500 N は JIS G 3101 SS400（板厚 16 mm 以下の降伏点 245 N/mm² 以上）から計算した出典付きの値。

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isic-2511 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:dev:physai-test → kbb -M:dev:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isic-2511 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。
