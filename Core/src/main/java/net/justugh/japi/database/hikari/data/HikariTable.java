package net.justugh.japi.database.hikari.data;

import lombok.Getter;
import net.justugh.japi.database.hikari.HikariAPI;
import net.justugh.japi.database.hikari.annotation.HikariStatementData;
import net.justugh.japi.database.hikari.redis.HikariRedisUpdateType;
import net.justugh.japi.database.hikari.redis.HikariUpdateRedisMessageListener;
import net.justugh.japi.database.redis.client.RedisClient;
import net.justugh.japi.database.redis.listener.ListenerComponent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public abstract class HikariTable<O extends HikariObject> {

    private final HikariAPI hikariAPI;
    private final String table;
    private final HikariTableStatements statements;
    private final List<O> data;
    private String idFieldName;

    private RedisClient redisClient;

    public HikariTable(HikariAPI hikariAPI, Class<O> objectClass, String table) {
        this.hikariAPI = hikariAPI;
        this.table = table;
        this.statements = null;
        data = new ArrayList<>();

        try {
            O tempObject = objectClass.newInstance();
            hikariAPI.executeStatement(tempObject.getTableCreateStatement());
            Map.Entry<String, HikariStatementData> data = tempObject.getRawStatementData().entrySet().stream().filter(entry -> entry.getValue().primaryKey()).findFirst().orElse(null);
            idFieldName = data == null ? null : data.getKey();
        } catch (InstantiationException | IllegalAccessException e) {
            System.out.println("[JustAPI] [HikariAPI] (" + table + "): Unable to create table.");
            e.printStackTrace();
        }

        loadData();
    }

    public HikariTable(HikariAPI hikariAPI, HikariTableStatements statements) {
        this.hikariAPI = hikariAPI;
        this.table = null;
        this.statements = statements;
        data = new ArrayList<>();
        hikariAPI.executeStatement(statements.getTableCreateStatement());
        loadData();
    }

    private void loadData() {
        data.clear();

        if (statements == null) {
            hikariAPI.executeQuery(String.format("SELECT * FROM %s;", table), result -> {
                if (result == null) {
                    return;
                }

                try {
                    while (result.next()) {
                        data.add(loadObject(result));
                    }
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            });
            return;
        }

        hikariAPI.executeQuery(statements.getDataStatement(), result -> {
            if (result == null) {
                return;
            }

            try {
                while (result.next()) {
                    data.add(loadObject(result));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void save(O object) {
        save(object, null);
    }

    public void save(O object, Consumer<ResultSet> result) {
        if (!object.getRawStatementData().isEmpty()) {
            hikariAPI.executeStatement(object.getDataSaveStatement(), result);

            if (redisClient != null) {
                redisClient.sendListenerMessage(new ListenerComponent(null, "hikari-update")
                        .addData("updateType", HikariRedisUpdateType.DELETE).addData("objectId", object.getDataId().toString()));
            }
            return;
        }

        hikariAPI.executeStatement(statements.getSaveStatement(), result, object.getDataObjects());

        if (redisClient != null) {
            redisClient.sendListenerMessage(new ListenerComponent(null, "hikari-update")
                    .addData("updateType", HikariRedisUpdateType.DELETE).addData("objectId", object.getDataId().toString()));
        }
    }

    public void delete(O object) {
        if (!object.getRawStatementData().isEmpty()) {
            hikariAPI.executeStatement(object.getDataDeleteStatement(), rs -> {
                if (redisClient != null) {
                    redisClient.sendListenerMessage(new ListenerComponent(null, "hikari-update")
                            .addData("updateType", HikariRedisUpdateType.DELETE).addData("objectId", object.getDataId().toString()));
                }
            });
            return;
        }

        hikariAPI.executeStatement(statements.getDeleteStatement(), rs -> {
            if (redisClient != null) {
                redisClient.sendListenerMessage(new ListenerComponent(null, "hikari-update")
                        .addData("updateType", HikariRedisUpdateType.DELETE).addData("objectId", object.getDataId().toString()));
            }
        }, object.getDataId());
    }

    public abstract O loadObject(ResultSet set) throws SQLException;

    public O getDataById(Object id) {
        return data.stream().filter(o -> o.getDataId().equals(id)).findFirst().orElse(null);
    }

    public void getDataFromDB(Object id, Consumer<O> consumer) {
        if (idFieldName == null) {
            return;
        }

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, idFieldName, id.toString()), result -> {
            if (result == null) {
                return;
            }

            try {
                while (result.next()) {
                    consumer.accept(loadObject(result));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void registerRedisHook(RedisClient redisClient) {
        this.redisClient = redisClient;
        redisClient.registerListener(new HikariUpdateRedisMessageListener<O>(this));
    }

}
