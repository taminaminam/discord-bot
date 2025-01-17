package icu.taminaminam.spideybot.utils.exceptions;

import icu.taminaminam.spideybot.utils.BotUtils;
import reactor.util.annotation.NonNull;

public class BotException extends Exception {
	
	private final String key;
	private final Object[] args;
	
	BotException(@NonNull String key, @NonNull Object... args){
		this.key = key;
		this.args = args;
	}
	
	@NonNull
	public String getErrorMessage(@NonNull String language){
		return BotUtils.getLanguageString(language, key, args);
	}
	
	/**
	 * @param key  The key which should be used to get the language string from the {@link java.util.ResourceBundle}
	 * @param args The arguments used to format the plain language string
	 * @return The new {@link InvalidArgumentException}
	 */
	@NonNull
	public static InvalidArgumentException invalidArgument(@NonNull String key, @NonNull Object... args){
		return new InvalidArgumentException(key, args);
	}
	
	/**
	 * @param key  The key which should be used to get the language string from the {@link java.util.ResourceBundle}
	 * @param args The arguments used to format the plain language string
	 * @return The new {@link MissingPermissionsException}
	 */
	@NonNull
	public static MissingPermissionsException missingPermissions(@NonNull String key, @NonNull Object... args){
		return new MissingPermissionsException(key, args);
	}
	
	/**
	 * @param key  The key which should be used to get the language string from the {@link java.util.ResourceBundle}
	 * @param args The arguments used to format the plain language string
	 * @return The new {@link NotExecutableException}
	 */
	@NonNull
	public static NotExecutableException notExecutable(@NonNull String key, @NonNull Object... args){
		return new NotExecutableException(key, args);
	}
	
	/**
	 * @param key  The key which should be used to get the language string from the {@link java.util.ResourceBundle}
	 * @param args The arguments used to format the plain language string
	 * @return The new {@link RatelimitedException}
	 */
	@NonNull
	public static RatelimitedException ratelimited(@NonNull String key, @NonNull Object... args){
		return new RatelimitedException(key, args);
	}
	
	@NonNull
	public static BotMissingPermissionsException botMissingPermissions(@NonNull String key, @NonNull Object... args){
		return new BotMissingPermissionsException(key, args);
	}

}
