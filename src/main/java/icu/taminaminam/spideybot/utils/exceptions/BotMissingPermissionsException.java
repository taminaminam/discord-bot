package icu.taminaminam.spideybot.utils.exceptions;

public class BotMissingPermissionsException extends BotException {
	BotMissingPermissionsException(String key, Object... args) {
		super(key, args);
	}
}
