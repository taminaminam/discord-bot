package icu.taminaminam.spideybot.utils.exceptions;

import icu.taminaminam.spideybot.commands.Command;
import reactor.util.annotation.NonNull;

/**
 * A {@link BotException} emitted by {@link reactor.core.publisher.Mono}s to indicate that the
 * {@link Command} can not be executed for any reason.
 */
public class NotExecutableException extends BotException {
	NotExecutableException(@NonNull String key, @NonNull Object... args) {
		super(key, args);
	}
}
