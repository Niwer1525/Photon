package niwer.photon.objects;

import niwer.queryon.SQLSerializable;
import niwer.queryon.tables.api.IColumnField;

/**
 * @author Niwer 
 */
public class ObjectRecoveryCode extends SQLSerializable<ObjectRecoveryCode> {

    @IColumnField(name = "codeHash", primaryKey = true, notNull = true, charLimit = 255)
    private String codeHash;

    @IColumnField(name = "uuid", notNull = true)
    private String uuid;

    public String getCodeHash() { return this.codeHash; }

    public String getUuid() { return this.uuid; }

    @Override
    public String toString() {
        return String.format("ObjectRecoveryCode{codeHash='%s', uuid='%s'}", this.codeHash, this.uuid);
    }
}
