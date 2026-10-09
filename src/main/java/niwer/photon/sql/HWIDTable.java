package niwer.photon.sql;

import niwer.photon.PhotonEngine;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.DeletionManager;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.tables.EnumColumnTypes;
import niwer.queryon.tables.Table;

public class HWIDTable extends Table {

    @Override
    public String name() { return "HWID"; }

    public HWIDTable(DataBase db) {
        super(db);

        this.addColumns(
            createColumn(db, "userUUID", EnumColumnTypes.TEXT).notNull().primaryKey(),
            createColumn(db, "userHWID", EnumColumnTypes.TEXT).notNull(),
            createColumn(db, "operatingSystem", EnumColumnTypes.TEXT).notNull()
        ).execute();
    }
    
    /**
     * Add or update a HWID entry in the database.
     * 
     * @param userUUID The user UUID
     * @param userHWID The hardware ID
     * @param operatingSystem The operating system
     */
    public static void save(String userUUID, String userHWID, String operatingSystem) {
        InsertionManager.insert(PhotonEngine.DATA_BASE, HWIDTable.class, "userUUID", "userHWID", "operatingSystem")
            .row(userUUID, userHWID, operatingSystem)
            .execute();
    }

    /**
     * Check if a HWID exists for a given user.
     * 
     * @param userUUID The user UUID
     * @return true if HWID exists
     */
    public static boolean exist(String userUUID) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, HWIDTable.class, "userHWID")
            .where(Expressions.isEqualTo("userUUID", userUUID))
            .limit(1)
            .executeHasResult();
    }

    /**
     * Get HWID for a specific user.
     * 
     * @param userUUID The user UUID
     * @return The hardware ID or null if not found
     */
    public static String getHWID(String userUUID) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, HWIDTable.class, "userHWID")
            .where(Expressions.isEqualTo("userUUID", userUUID))
            .limit(1)
            .executePrimitive(String.class);
    }

    /**
     * Delete a HWID entry.
     * 
     * @param userUUID The user UUID
     * @param userHWID The hardware ID
     */
    public static void deleteHWID(String userUUID, String userHWID) {
        DeletionManager.delete(PhotonEngine.DATA_BASE, HWIDTable.class)
            .where(
                Expressions.and(
                    Expressions.isEqualTo("userUUID", userUUID),
                    Expressions.isEqualTo("userHWID", userHWID)
                )
            )
            .execute();
    }
}