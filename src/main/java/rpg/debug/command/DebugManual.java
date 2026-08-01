package rpg.debug.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import rpg.core.command.Pagination;
import rpg.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders {@code /oladmin manual [page]} - an in-game summary of every debug subcommand,
 * mirroring {@code orelia-debug/README.md}.
 */
final class DebugManual {

    private static final int ENTRIES_PER_PAGE = 6;

    private record Entry(String usage, String description) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("oladmin gui <status|equipment|skill|job|shop|warehouse|crafting|auction|mail|ranking|house|pet|achievement|dungeon> [player]",
                    "指定プレイヤー(省略時は自分)に各種GUIを強制表示します。dungeonはOreliaWorld、auction/mail/ranking/house/pet/achievementはOreliaExtra導入時のみ使用可能です。"),
            new Entry("oladmin money <give|set|take> [player] <amount>",
                    "指定プレイヤー(省略時は自分)の所持金を付与・設定・引き出しします。"),
            new Entry("oladmin skillpoints <give|set|take> [player] <amount>",
                    "指定プレイヤー(省略時は自分)のスキル習得ポイントを付与・設定・引き出しします。"),
            new Entry("oladmin debugmode <on|off|toggle> [player]",
                    "指定プレイヤー(省略時は自分)のデバッグモードを切り替えます。有効な間は武器の職業/レベル要件、スキルの武器種一致・ソケット済み・習得済み・クールダウン・SPコストを全て無視して使用できます。"),
            new Entry("oladmin exp give [player] <amount>",
                    "指定プレイヤー(省略時は自分)に経験値を付与します。"),
            new Entry("oladmin relic give [player] <dungeonId>",
                    "指定プレイヤー(省略時は自分)にレリックを1個付与します（relics.ymlのそのダンジョンIDのプールから抽選）。"),
            new Entry("oladmin config <core|world|extra> list",
                    "対象プラグインの設定ファイル一覧を表示します。"),
            new Entry("oladmin config <core|world|extra> get <file> <path>",
                    "設定ファイルの値を確認します。"),
            new Entry("oladmin config <core|world|extra> set <file> <path> <value>",
                    "設定ファイルの値を変更し即座に保存します。boolean/数値/文字列を自動判定します。"),
            new Entry("oladmin config <core|world|extra> save <file>",
                    "設定ファイルを手動で保存します。"),
            new Entry("oladmin confighelp <core|world|extra> <file>",
                    "設定ファイルの全キー一覧を表示します。"),
            new Entry("oladmin quest complete [player] <questId>",
                    "指定プレイヤー(省略時は自分)のクエストの目標を強制達成します（要OreliaWorld）。報告自体は対象プレイヤーが /ol quest から行います。"),
            new Entry("oladmin quest start [player] <questId>",
                    "前提条件・レベル制限を無視してクエストを強制受注させます（要OreliaWorld）。"),
            new Entry("oladmin quest resetcooldown [player] <questId>",
                    "クエストの完了記録をクリアし、即座に再受注できるようにします（要OreliaWorld）。"),
            new Entry("oladmin quest list [player]|ids",
                    "指定プレイヤー(省略時は自分)の受注中クエスト、またはquests.ymlの全クエストIDを一覧表示します（要OreliaWorld）。"),
            new Entry("oladmin title list [player]",
                    "指定プレイヤー(省略時は自分)の取得済み称号と装備中の称号を表示します（要OreliaWorld）。"),
            new Entry("oladmin title grant [player] <title>",
                    "クエスト報酬を経由せず、指定プレイヤー(省略時は自分)に称号を直接付与します（要OreliaWorld）。"),
            new Entry("oladmin title equip [player] <title>",
                    "指定プレイヤー(省略時は自分)に称号を強制装備させます。未取得の称号でも装備できます（表示プレビュー用、要OreliaWorld）。"),
            new Entry("oladmin title unequip [player]",
                    "指定プレイヤー(省略時は自分)の称号を解除します（要OreliaWorld）。"),
            new Entry("oladmin dungeon unlock [player] <dungeonId>",
                    "指定プレイヤー(省略時は自分)にダンジョンを開放させます（要OreliaWorld）。"),
            new Entry("oladmin dungeon forcestart [player] <dungeonId>",
                    "開放チェックを無視して、指定プレイヤー(省略時は自分)にダンジョンをソロで強制開始させます（要OreliaWorld）。"),
            new Entry("oladmin dungeon forceend [player]",
                    "指定プレイヤー(省略時は自分)が挑戦中のダンジョンを強制終了します（要OreliaWorld）。"),
            new Entry("oladmin dungeon status [player]|ids",
                    "指定プレイヤー(省略時は自分)の現在のダンジョン挑戦状況、またはdungeons.ymlの全ダンジョンIDを表示します（要OreliaWorld）。"),
            new Entry("oladmin npc create <id> <type> [entityType]|move <id>|remove <id>|list [page]",
                    "NPCの設置・移動・削除を行うコマンドです（OreliaWorld本体）。"),
            new Entry("oladmin pet unlock [player] <petId>|list [player]|ids",
                    "指定プレイヤー(省略時は自分)に経済チェック無しでペットを付与、所持ペット一覧、またはpets.ymlの全ペットIDを表示します（要OreliaExtra）。"),
            new Entry("oladmin mount unlock [player] <mountId>|list [player]|ids",
                    "指定プレイヤー(省略時は自分)に経済チェック無しでマウントを付与、所持マウント一覧、またはmounts.ymlの全マウントIDを表示します（要OreliaExtra）。"),
            new Entry("oladmin house grant [player] <plotId>|clear [player]|status [player]|ids",
                    "指定プレイヤー(省略時は自分)に経済チェック無しで住居プロットを付与・解除、所持状況、またはhousing.ymlの全プロットIDを表示します（要OreliaExtra）。"),
            new Entry("oladmin trade status [player]|forcecancel [player]",
                    "指定プレイヤー(省略時は自分)の取引状況を確認、または詰まった取引を強制キャンセル(アイテム返却)します（要OreliaExtra）。")
    );

    private DebugManual() {
    }

    static void send(CommandSender sender, String rawPage) {
        int page = parsePageOrDefault(rawPage);
        List<Component> lines = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            lines.add(ColorUtil.component("&%e/" + entry.usage()));
            lines.add(ColorUtil.component("&%7  " + entry.description()));
        }
        Pagination.send(sender, "&%6&lOreliaDebug コマンド一覧&%7 ({page}/{total}ページ)",
                lines, ENTRIES_PER_PAGE * 2, page, "/oladmin manual");
    }

    private static int parsePageOrDefault(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
