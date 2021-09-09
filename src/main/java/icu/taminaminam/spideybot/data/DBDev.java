package icu.taminaminam.spideybot.data;

import discord4j.common.util.Snowflake;
import icu.taminaminam.spideybot.utils.BotUtils;
import io.r2dbc.spi.Row;
import reactor.util.annotation.NonNull;

public class DBDev {

    private final Snowflake id;
    private final boolean isDev;
    private final boolean isOwner;
    private final boolean isListed;
    private final String devRole;


    private DBDev(@NonNull Snowflake id, @NonNull boolean isDev, @NonNull boolean isOwner, @NonNull boolean isListed, @NonNull String devRole){
        this.id = id;
        this.isDev = isDev;
        this.isOwner = isOwner;
        this.isListed = isListed;
        this.devRole = devRole;
    }

    //Getters
    @NonNull public Snowflake getId(){ return id; }
    @NonNull public boolean isDev() { return isDev; }
    @NonNull public boolean isOwner() { return isOwner; }
    @NonNull public boolean isListed() { return isListed; }
    @NonNull public String getDevRole() { return devRole; }

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
                row.get("devRole", String.class)
        );
    }
}
