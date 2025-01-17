package icu.taminaminam.spideybot.data;

import icu.taminaminam.spideybot.utils.BotUtils;
import discord4j.common.util.Snowflake;
import io.r2dbc.spi.Row;
import reactor.util.annotation.NonNull;

public class DBUser {
	
	public static final DBUser defaultUser = new DBUser(Snowflake.of(0), BotUtils.DEFAULT_PREFIX, "en");
	
	private final Snowflake id;
	private final String prefix;
	private final String language;
	
	private DBUser(@NonNull Snowflake id, @NonNull String prefix, @NonNull String language){
		this.id = id;
		this.prefix = prefix;
		this.language = language;
	}
	
	@NonNull public Snowflake getId(){ return id; }
	@NonNull public String getPrefix(){ return prefix; }
	@NonNull public String getLanguage() { return language; }
	
	/**
	 * @param row The {@link Row} to get the data from
	 * @return A new {@link DBUser} based on the values of the {@link Row}
	 */
	@NonNull
	public static DBUser ofRow(@NonNull Row row){
		return new DBUser(
				Snowflake.of(row.get("userId", Integer.class)),
				row.get("prefix", String.class),
				row.get("language", String.class)
		);
	}
	
}
