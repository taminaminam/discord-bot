package icu.taminaminam.spideybot.data;

import discord4j.common.util.Snowflake;
import icu.taminaminam.spideybot.utils.BotUtils;
import io.r2dbc.spi.Row;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

public class DBDev {

    public static final DBDev defaultDev = new DBDev(Snowflake.of(0), true, false, true, "junior Dev", "Active");

    private final Snowflake id;
    private final boolean isDev;
    private final boolean isOwner;
    private final boolean isListed;
    private final String devRole;
    private final String status;


    private DBDev(@NonNull Snowflake id, @NonNull boolean isDev, @NonNull boolean isOwner, @NonNull boolean isListed, @NonNull String devRole, @NonNull String status){
        this.id = id;
        this.isDev = isDev;
        this.isOwner = isOwner;
        this.isListed = isListed;
        this.devRole = devRole;
        this.status = status;
    }

    //Getters
    @NonNull public Snowflake getId(){ return this.id; }
    @NonNull public boolean isDev() { return this.isDev; }
    @NonNull public boolean isOwner() { return this.isOwner; }
    @NonNull public boolean isListed() { return this.isListed; }
    @NonNull public String getDevRole() { return this.devRole; }
    @NonNull public String getStatus() { return this.status; }
    /**
     * @param row The {@link Row} to get the data from
     * @return A new {@link DBDev} based on the values of the {@link Row}
     */
    @NonNull
    public static DBDev ofRow(@NonNull Row row){
        return new DBDev(
                Snowflake.of(row.get("userId", Integer.class)),
                row.get("isDev", Boolean.class),
                row.get("isOwner", Boolean.class),
                row.get("isListed", Boolean.class),
                row.get("devRole", String.class),
                row.get("status", String.class)
        );
    }


}


