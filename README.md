<img src="https://orelia-mc.github.io/assets/logo_wide.jpg" />
<h1 align="center">Orelia Debug</h1>
<p align="center">Debug plugin for Orellia-MC plugins</p>

Orelia RPGプラグイン群（orelia-core / orelia-world / orelia-extra）のテストプレイを助けるための管理者向けデバッグツールです。本番サーバーへの導入は必須ではなく、開発・検証環境にのみ導入することを想定しています。

## 依存関係

- **orelia-core**（必須）: `plugin.yml` の `depend` に指定。導入されていないとプラグインは無効化されます。
- **orelia-world**（任意）: 未導入でも起動しますが、`debug quest` / `debug npc` / `debug config world ...` などworld関連の機能は使用できません。
- **orelia-extra**（任意）: 未導入でも起動しますが、`debug gui auction|mail|ranking` / `debug config extra ...` などextra関連の機能は使用できません。
- **Vault**（任意）: orelia-core自身がVault経済プロバイダとして動作するため、直接の依存はありません。

## ビルド

```
./gradlew build
```

`build/libs/orelia-debug-1.0.0.jar` が生成されます。他の3プラグインと同じく `repo.papermc.io` / `jitpack.io` へのネットワークアクセスが必要です。

## コマンド一覧

すべて `/oladmin debug <サブコマンド>` の形で、orelia-coreが公開する共有の `AdminCommandRegistry` に登録されます（`orelia.admin` 権限、デフォルトop限定）。

### GUI強制表示

```
/oladmin debug gui <status|equipment|skill|job|shop|warehouse|auction|mail|ranking> <player>
```

指定したプレイヤーに対して、各種GUI画面を通常のプレイ導線（NPC接触やコマンド）を経由せず直接開きます。`shop` は在庫なしの状態で開きます。`auction` / `mail` / `ranking` はOreliaExtra導入時のみ使用できます。

### 所持金操作

```
/oladmin debug money <give|set|take> <player> <amount>
```

指定プレイヤーの所持金を付与・設定・引き出しします。`take` は残高不足の場合失敗します。

### 経験値付与

```
/oladmin debug exp give <player> <amount>
```

### config編集

```
/oladmin debug config <core|world|extra> list
/oladmin debug config <core|world|extra> get <file> <path>
/oladmin debug config <core|world|extra> set <file> <path> <value>
/oladmin debug config <core|world|extra> save <file>
/oladmin debug confighelp <core|world|extra> <file>
```

`core` / `world` / `extra` は対象プラグインを指定します。`path` はYAMLのドット区切りパス（例: `economy.starting-balance`）。`set` の値は `true`/`false` → boolean、数値として解釈できれば long/double、それ以外は文字列として自動判定されます。`set` は即座にファイルへ保存されます。

### クエスト関連（要OreliaWorld）

```
/oladmin debug quest complete <player> <questId>
```

受注中のクエストの全目標を強制的に達成状態にします（`AWAITING_REPORT` へ遷移）。報酬付与そのものは対象プレイヤーが `/ol quest` からNPCへ報告して受け取る必要があります。

### NPC一覧（要OreliaWorld）

```
/oladmin debug npc
```

登録されているNPCのIDを一覧表示します。NPCの設置・移動・削除自体は `orelia-debug` ではなく `orelia-world` 本体の `/oladmin npc create|move|remove|list` コマンドで行います。

### マニュアル

```
/oladmin debug manual [page]
```

このREADMEの内容をゲーム内でページ送り表示します。

## 開発時の注意（mavenLocal依存）

`build.gradle.kts` の `repositories` に一時的に `mavenLocal()` を追加しています。orelia-core/orelia-world/orelia-extraを並行して変更している間は、それぞれのリポジトリで `./gradlew publishToMavenLocal` を実行してから本プラグインをビルドしてください。本番リリース前にはこの行を削除し、jitpack経由の解決のみに戻す想定です。
