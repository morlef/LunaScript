# LunaScript Editor

LunaScript Editorは、ビジュアルノベルやアドベンチャーゲーム風の演出を作成するためのスクリプトエディタです。

## 必要要件

- JDK 21 以上
- Windows 10 以上 (デバッグ環境、他OSでもJVM環境があれば動作可能)

## 主な機能

- スクリプト編集: コマンドブロックによる直感的な演出編集
- プレビュー機能: 作成したシーンをその場で再生確認
- アセット管理: 画像や音声ファイルをドラッグ＆ドロップで指定可能
- 分岐・制御: IF/ELSE分岐、フラグ管理、選択肢の実装
- 演出機能:
  - 背景、立ち絵、画像の表示・フェード・移動・拡大縮小・回転
  - BGM、SEの再生・フェード
  - 画面シェイク、フラッシュ、色調補正（Tint）
  - テキスト表示、タイプライター演出

- テキスト表示では、リッチテキストタグとして、<b>, <i>, <u>, <s>, <color>, <size>が使用できます。

## 実行方法

### ソースコードから実行する場合
プロジェクトルートで以下のコマンドを実行してください。

```powershell
.\gradlew run
```

### jarファイルのビルド
配布用の単一jarファイルを生成する場合:

```powershell
.\gradlew shadowJar
```

`build/libs` フォルダに生成された `LunaScriptEditor-0.1-all.jar` (バージョン等は設定に依存) を実行します。

```powershell
java -jar build/libs/LunaScriptEditor-0.1-all.jar
```

JDK 21のインストール環境でダブルクリックしたら起動できます。

## ディレクトリ構成とアセット

実行ファイル（またはプロジェクトルート）と同じ階層に `assets` フォルダを配置し、その中にリソースを格納します。

```text
ProjectRoot/
  ├─ assets/
  │   ├─ bg/             ... 背景画像 (jpg, png)
  │   ├─ bgm/            ... BGMファイル (ogg, mp3, wav)
  │   ├─ se/             ... 効果音ファイル (ogg, mp3, wav)
  │   ├─ characters/     ... 立ち絵画像
  │   ├─ image/          ... その他演出用画像
  │   ├─ letter_se.wav   ... テキスト表示用SE
  │   └─ characters.txt  ... 立ち絵の名前/差分一覧
  └─ ...
```

## LunaScriptについて
スクリプト仕様の詳細は、ルートディレクトリの `README.md` を参照してください。
