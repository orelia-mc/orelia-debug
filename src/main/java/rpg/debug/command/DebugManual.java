package rpg.debug.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import rpg.core.message.MessageManager;

import java.util.List;

/**
 * Renders {@code /oladmin debug manual [page]} - an in-game summary of every debug
 * subcommand, mirroring {@code orelia-debug/README.md}.
 */
final class DebugManual {

    private static final int PAGE_SIZE = 6;
    private static final String DIVIDER = ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH
            + "----------------------------------------";

    private record Entry(String usage, String description) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("oladmin debug gui <status|equipment|skill|job|shop|warehouse|auction|mail|ranking> <player>",
                    "指定プレイヤーに各種GUIを強制表示します。auction/mail/rankingはOreliaExtra導入時のみ使用可能です。"),
            new Entry("oladmin debug money <give|set|take> <player> <amount>",
                    "指定プレイヤーの所持金を付与・設定・引き出しします。"),
            new Entry("oladmin debug exp give <player> <amount>",
                    "指定プレイヤーに経験値を付与します。"),
            new Entry("oladmin debug config <core|world|extra> list",
                    "対象プラグインの設定ファイル一覧を表示します。"),
            new Entry("oladmin debug config <core|world|extra> get <file> <path>",
                    "設定ファイルの値を確認します。"),
            new Entry("oladmin debug config <core|world|extra> set <file> <path> <value>",
                    "設定ファイルの値を変更し即座に保存します。boolean/数値/文字列を自動判定します。"),
            new Entry("oladmin debug config <core|world|extra> save <file>",
                    "設定ファイルを手動で保存します。"),
            new Entry("oladmin debug confighelp <core|world|extra> <file>",
                    "設定ファイルの全キー一覧を表示します。"),
            new Entry("oladmin debug quest complete <player> <questId>",
                    "クエストの目標を強制達成します（要OreliaWorld）。報告自体は対象プレイヤーが /ol quest から行います。"),
            new Entry("oladmin debug npc",
                    "登録されているNPC一覧（ID）を表示します（要OreliaWorld）。"),
            new Entry("oladmin npc create <id> <type> [entityType]|move <id>|remove <id>|list [page]",
                    "NPCの設置・移動・削除を行うコマンドです（OreliaWorld本体、debugサブコマンドではありません）。")
    );

    private DebugManual() {
    }

    static void send(CommandSender sender, MessageManager messages, String rawPage) {
        int page = parsePageOrDefault(rawPage);
        int totalPages = Math.max(1, (ENTRIES.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int clampedPage = Math.min(Math.max(page, 1), totalPages);

        sender.sendMessage(DIVIDER);
        messages.send(sender, "manual.header", "page", clampedPage, "total", totalPages);
        sender.sendMessage(DIVIDER);

        int fromIndex = (clampedPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, ENTRIES.size());
        for (Entry entry : ENTRIES.subList(fromIndex, toIndex)) {
            sender.sendMessage(ChatColor.YELLOW + "/" + entry.usage());
            sender.sendMessage(ChatColor.GRAY + "  " + entry.description());
        }

        sender.sendMessage(DIVIDER);
        if (clampedPage < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "次のページ: " + ChatColor.WHITE + "/oladmin debug manual " + (clampedPage + 1));
        }
    }

    private static int parsePageOrDefault(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
