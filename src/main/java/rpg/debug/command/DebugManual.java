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
            new Entry("oladmin gui <status|equipment|skill|job|shop|warehouse|auction|mail|ranking> [player]",
                    "指定プレイヤー(省略時は自分)に各種GUIを強制表示します。auction/mail/rankingはOreliaExtra導入時のみ使用可能です。"),
            new Entry("oladmin money <give|set|take> [player] <amount>",
                    "指定プレイヤー(省略時は自分)の所持金を付与・設定・引き出しします。"),
            new Entry("oladmin exp give [player] <amount>",
                    "指定プレイヤー(省略時は自分)に経験値を付与します。"),
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
            new Entry("oladmin npc create <id> <type> [entityType]|move <id>|remove <id>|list [page]",
                    "NPCの設置・移動・削除を行うコマンドです（OreliaWorld本体）。")
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
