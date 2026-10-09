package niwer.photon.sql;

import java.util.List;

import net.dv8tion.jda.api.audit.ActionType;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectDiscordLog;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.DeletionManager;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.SelectionManager.EnumOrder;
import niwer.queryon.tables.Table;

public class DiscordLogTable extends Table {

    public static enum ModerationType {
        BAN, UNBAN, KICK, TIMEOUT;

        public ActionType toDiscordActionType() {
            switch (this) {
                case BAN: return ActionType.BAN;
                case UNBAN: return ActionType.UNBAN;
                case KICK: return ActionType.KICK;
                case TIMEOUT: return ActionType.MEMBER_UPDATE; // No direct equivalent, using MEMBER_UPDATE for timeout actions
                default: throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }

    public DiscordLogTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectDiscordLog.class).execute();
    }

    @Override public String name() { return "DiscordLog"; }

    /**
     * @param guildID            The Discord guild ID
     * @param discordID          The target user's Discord ID
     * @param type               The type of moderation action
     * @param reason             The reason for the action (nullable)
     * @param moderatorDiscordID The moderator's Discord ID (nullable)
     * @param durationSeconds    Duration in seconds for TIMEOUT, 0 otherwise
     */
    public static void save(String guildID, String discordID, ModerationType type, String reason, String moderatorDiscordID, long durationSeconds) {
        InsertionManager.insert(PhotonEngine.DATA_BASE, DiscordLogTable.class, "guild_id", "discord_user_id", "moderation_type", "reason", "moderator_discord_id", "duration_seconds")
            .row(guildID, discordID, type.name(), reason, moderatorDiscordID, durationSeconds)
            .execute();
    }

    public static List<ObjectDiscordLog> getByDiscordUserID(String discordUserID) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, DiscordLogTable.class)
            .where(Expressions.isEqualTo("discord_user_id", discordUserID))
            .orderBy("timestamp", EnumOrder.ASC)
            .executeList(ObjectDiscordLog.class);
    }

    public static List<ObjectDiscordLog> getByGuild(String guildID) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, DiscordLogTable.class)
            .where(Expressions.isEqualTo("guild_id", guildID))
            .orderBy("timestamp", EnumOrder.DESC)
            .executeList(ObjectDiscordLog.class);
    }

    public static List<ObjectDiscordLog> getByGuildAndType(String guildID, ModerationType type) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, DiscordLogTable.class)
            .where(Expressions.and(
                Expressions.isEqualTo("guild_id", guildID),
                Expressions.isEqualTo("moderation_type", type)
            ))
            .orderBy("timestamp", EnumOrder.DESC)
            .executeList(ObjectDiscordLog.class);
    }

    public static void deleteByDiscordID(String discordID) {
        DeletionManager.delete(PhotonEngine.DATA_BASE, DiscordLogTable.class)
            .where(Expressions.isEqualTo("discord_user_id", discordID))
            .execute();
    }
}