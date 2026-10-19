package niwer.photon.objects;

import java.util.Date;

import niwer.photon.util.GsonUtils;
import niwer.queryon.SQLSerializable;
import niwer.queryon.tables.api.IColumnField;

/**
 * @author Niwer 
 */
public class ObjectServer extends SQLSerializable<ObjectServer> {

    @IColumnField(name = "id", primaryKey = true, autoIncrement = true)
    public int id;

    @IColumnField(name = "server_name", unique = true)
	public String serverName;

    @IColumnField(name = "server_motd", charLimit = 2048)
	public String serverMOTD;
	
    @IColumnField(name = "server_ip")
    public String serverIP;

    @IColumnField(name = "server_port")
    public int serverPort;

    @IColumnField(name = "queue_port")
    public int queuePort;

    @IColumnField(name = "last_seen_at", notNull = true)
    public Date last_seen_at;
    
    @IColumnField(name = "site_url")
    public String site;
    
    @IColumnField(name = "discord")
    public String discord;

    @IColumnField(name = "project_id")
    public String projectId;

    @Override
    public int hashCode() {
        return (serverIP != null ? serverIP.hashCode() : 0) * 31 + serverPort;
    }

    public static ObjectServer fromPayload(String payload) {
        try {
            return GsonUtils.GSON.fromJson(payload, ObjectServer.class);
        } catch (Exception e) {
            return null;
        }
    }
}
