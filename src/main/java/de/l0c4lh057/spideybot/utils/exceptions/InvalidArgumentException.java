package de.l0c4lh057.spideybot.utils.exceptions;

import reactor.util.annotation.NonNull;

/**
 * A {@link BotException} emitted by {@link reactor.core.publisher.Mono}s to indicate that the user did not provide
 * correct arguments when executing a {@link de.l0c4lh057.spideybot.commands.Command}
 */
public class InvalidArgumentException extends BotException {
	InvalidArgumentException(@NonNull String key, @NonNull Object... args) {
		super(key, args);
	}
}
