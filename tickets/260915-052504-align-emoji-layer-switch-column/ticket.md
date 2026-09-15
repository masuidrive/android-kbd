---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "絵文字レイヤー左端へ日本語レイヤーと同じ縦並びのレイヤー切替列を追加する"
created_at: "2026-09-15T05:25:04Z"
started_at: 2026-09-15T05:28:47Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260915-052504-align-emoji-layer-switch-column

### Why
絵文字レイヤーだけレイヤー切替が最下段の`AZ`に偏っており、日本語レイヤーで覚えた左端の縦並び操作を使えない。Product Briefの「5つのレイヤーをフリックで切り替える」操作を、絵文字選択中も同じ位置関係で使えるようにする。

### What / Acceptance Criteria
この ticket が終わると、絵文字を選ぶ人が、日本語レイヤーと同じ左端の位置関係から他レイヤーへ直接移動できる。
- [x] 絵文字レイヤー左端に上から`☺`、`#!`、`19`、`AZ`が縦に並び、`#!`で記号、`19`でテンキー、`AZ`でQWERTYへ切り替わる。
- [x] 絵文字カテゴリ行、Recentを含む絵文字選択、縦スクロール、バックスペースが従来どおり使える。
- [x] スマホとtablet/dual幅で4行のキーボード高さ、キー間隔、最下部safe areaが他レイヤーから切り替えても変わらず、絵文字と切替列が重ならない。

### Architectural Invariants check
入力データを端末外へ送らず、custom Viewの座標と入力sessionを一元管理する既存invariantと矛盾しない。

### Design Decisions
- 左列は日本語レイヤーと同じ4段・同じラベル順にし、現在地の`☺`も残す。
- AndroidX EmojiPickerのカテゴリ行は全幅のまま残し、絵文字bodyだけを左列の右へ7列で配置する。
- 最下段は`AZ`を左端、バックスペースを右端に残す。

### Out-of-scope
- 絵文字カテゴリ、収録絵文字、Recent件数、検索機能は変更しない。
- レイヤー切替のフリック割当や他レイヤーのキー配置は変更しない。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指定: 「絵文字レイヤーの左には かなレイヤー と同じように縦にレイヤー変更並べて」。

### Dependencies
なし。リアルタイム音声波形を統合済みのmainを基準にする。

---
Work notes: `note.md`
