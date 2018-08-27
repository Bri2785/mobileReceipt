// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.repository;

import com.evnt.client.common.EVEManager;
import com.evnt.client.common.EVEManagerUtil;
import com.evnt.eve.event.EVEvent;
import com.evnt.util.KeyConst;
import com.fbi.fbo.impl.dataexport.DataExportResult;
import com.fbi.plugins.briteideas.data.MobileReceipt;
import com.fbi.plugins.briteideas.data.MobileReceiptItem;
import com.evnt.common.MethodConst;
import org.slf4j.LoggerFactory;
import java.io.Writer;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
//import com.fishbowl.hub.db.api.shipping.CartonItem;
//import com.fishbowl.hub.db.api.shipping.Carton;
//import com.fbi.commerce.shipping.util.Compatibility;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import com.evnt.util.Util;
import com.fbi.fbo.impl.dataexport.QueryRow;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.slf4j.Logger;

public class MRRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(MRRepository.class);
    private static final int FIRE_BIRD_LIST_LIMIT = 1500;
    private static int databaseVersion = -1;
    public static final int FB_VERSION_2017_01 = 100;
    private final MRRepository.RunSql sql;
    private final Map<String, String> storedSql = new ConcurrentHashMap();

    public MRRepository(MRRepository.RunSql sql) {
        this.sql = sql;
    }



    public List<QueryRow> getActiveMobileRecipts() {
        return this.sql.executeSql(this.loadSql("getActiveMobileReceipts.sql"));
    }


    public List<MobileReceipt> getMobileReceiptDataList(){
        List<MobileReceipt> data = new ArrayList<>();
        for (QueryRow row: this.getActiveMobileRecipts()
                ) {
            data.add(new MobileReceipt(
                    row.getInt("id"),
                    row.getInt("mr_id"),
                    row.getString("description"),
                    row.getDate("time_started"),
                    row.getDate("time_finished"),
                    row.getDate("time_uploaded")
            ));
        }
        return data;
    }

    public List<QueryRow> getMobileReciptItems(int receiptId) {
        return this.sql.executeSql(this.loadSql("getMobileReceiptItems.sql", receiptId));
    }

    public List<MobileReceiptItem> getMobileReceiptItemsbyReceipt(int receiptId){
        for (QueryRow row: this.getMobileReciptItems(receiptId)
             ) {
            LOGGER.error("Row " + row);
        }

        try {
            List<MobileReceiptItem> data = new ArrayList<>();
            for (QueryRow row : this.getMobileReciptItems(receiptId)
                    ) {
                data.add(new MobileReceiptItem(

                        row.getInt("mrid"),
                        row.getString("upc"),
                        row.getDate("lastDateScanned"),
                        //row.getInt("statusId"),
                        row.getDouble("qtyScanned"),
                        row.getInt("productId"),
                        row.getString("num"),
                        row.getInt("partId"),
                        row.getDouble("multiply"),
                        row.getDouble("totalReceived")
                ));
            }
            return data;
        }
        catch (Exception e){
            LOGGER.error("E",e);
        }
        return null;
    }




    public String getProperty(String key) {
        List<QueryRow> result = this.sql.executeSql(this.loadSql(Compatibility.handleFirebirdCompatibility(this, "getProperty.sql"), quote(key), quote("ShipExtend")));
        return Util.isEmpty(result) ? "" : ((QueryRow)result.get(0)).getString("info");
    }

    public synchronized int getDatabaseVersion() {
        if (databaseVersion == -1) {
            databaseVersion = ((QueryRow)this.sql.executeSql("SELECT MAX(version) AS version FROM databaseVersion ").get(0)).getInt("version").intValue();
        }

        return databaseVersion;
    }

    private String loadSql(String fileName, Object... params) {
        String sqlString = (String)this.storedSql.computeIfAbsent(fileName, (k) -> {
            try {
                InputStream stream = MRRepository.class.getResourceAsStream(k);
                Throwable var2 = null;

                Object var7;
                try {
                    InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                    Throwable var4 = null;

                    try {
                        Writer writer = new StringWriter();
                        Throwable var6 = null;

                        try {
                            while(reader.ready()) {
                                writer.write(reader.read());
                            }

                            var7 = writer.toString();
                        } catch (Throwable var54) {
                            var7 = var54;
                            var6 = var54;
                            throw var54;
                        } finally {
                            if (writer != null) {
                                if (var6 != null) {
                                    try {
                                        writer.close();
                                    } catch (Throwable var53) {
                                        var6.addSuppressed(var53);
                                    }
                                } else {
                                    writer.close();
                                }
                            }

                        }
                    } catch (Throwable var56) {
                        var4 = var56;
                        throw var56;
                    } finally {
                        if (reader != null) {
                            if (var4 != null) {
                                try {
                                    reader.close();
                                } catch (Throwable var52) {
                                    var4.addSuppressed(var52);
                                }
                            } else {
                                reader.close();
                            }
                        }

                    }
                } catch (Throwable var58) {
                    var2 = var58;
                    throw var58;
                } finally {
                    if (stream != null) {
                        if (var2 != null) {
                            try {
                                stream.close();
                            } catch (Throwable var51) {
                                var2.addSuppressed(var51);
                            }
                        } else {
                            stream.close();
                        }
                    }

                }

                return (String)var7;
            } catch (IOException var60) {
                LOGGER.error(var60.getMessage(), var60);
                LOGGER.error("Could not load file: {}", k);
                return "";
            }
        });
        return String.format(sqlString, this.paramsToString(params));
    }

    private void executePrivledgedSql(String query){
        EVEManager eveManager = EVEManagerUtil.getEveManager();
        final EVEvent request = eveManager.createRequest(MethodConst.RUN_DATA_EXPORT);
        request.add((Object) KeyConst.DATA_EXPORT_QUERY, query);
        request.add((Object)KeyConst.SUPPORT_LOGGED_IN, true);
        final EVEvent response = eveManager.sendAndWait(request);
        DataExportResult queryData = (DataExportResult)response.getObject((Object)KeyConst.DATA_EXPORT_RESULTS, (Class)DataExportResult.class);

    }

    private Object[] paramsToString(Object[] params) {
        if (params == null) {
            return new Object[0];
        } else {
            Object[] strings = new String[params.length];

            for(int i = 0; i < params.length; ++i) {
                if (params[i] == null) {
                    strings[i] = "NULL";
                } else if (params[i] instanceof List) {
                    List<Object> list = (List)params[i];
                    if (list.isEmpty()) {
                        strings[i] = "NULL";
                    } else {
                        strings[i] = (String)list.stream().map(String::valueOf).collect(Collectors.joining(","));
                    }
                } else {
                    strings[i] = params[i].toString();
                }
            }

            return strings;
        }
    }

    private static String quote(Object o) {
        return "'" + o.toString().replaceAll("'", "''") + "'";
    }

    private static String escape(Object o) {
        return "'" + o.toString().replaceAll("([|_%;])", "|$1").replaceAll("'", "''") + "'";
    }

    private QueryRow getSingleResult(List<QueryRow> queryRowList) {
        return queryRowList.isEmpty() ? null : (QueryRow)queryRowList.get(0);
    }

    private static <T> List<T> page(List<T> l, int start) {
        return l.subList(start, Math.min(l.size(), start + 1500));
    }

    @FunctionalInterface
    public interface RunSql {
        List<QueryRow> executeSql(String var1);
    }
}
