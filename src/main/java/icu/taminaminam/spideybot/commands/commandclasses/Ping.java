package icu.taminaminam.spideybot.commands.commandclasses;

import discord4j.core.object.entity.Message;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import icu.taminaminam.spideybot.commands.Command;
import icu.taminaminam.spideybot.utils.ratelimits.Ratelimit;
import icu.taminaminam.spideybot.utils.ratelimits.RatelimitFactory;
import icu.taminaminam.spideybot.utils.ratelimits.RatelimitType;
import io.github.bucket4j.Bandwidth;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class Ping extends Command {
    public Ping(){
        super();
    }

    @NonNull
    @Override
    public String getName() {
        return "ping";
    }

    /**
     * @return All the aliases of the command (does not include the name returned by {@link #getName()})
     */
    @NonNull
    @Override
    public String[] getAliases() {
        return new String[]{"pong"};
    }

    @NonNull
    @Override
    public Category getCategory() {
        return Category.GENERAL;
    }

    @NonNull
    @Override
    public CommandExecutor getExecutor() {
        return (context, language, prefix, args) -> {

            StringBuilder responseBuilder = new StringBuilder();

            if (context.getMessageContent().toLowerCase(Locale.ROOT).startsWith(prefix+"ping")) {
                responseBuilder.append("Pong!");
            }
            else if (context.getMessageContent().toLowerCase(Locale.ROOT).startsWith(prefix+"pong")){
                responseBuilder.append("Ping!");
            } else {
                responseBuilder.append("Bloop!");
            }

            if (args.contains("time")){
                Message msg = context.getMessage();

                Instant msgTime = msg.getTimestamp();

                Instant now = Instant.now();

                long timeElapsed = Duration.between(msgTime, now).toMillis();


                responseBuilder.append(" ");
                responseBuilder.append("(");
                responseBuilder.append(timeElapsed);
                responseBuilder.append("ms");
                responseBuilder.append(")");
            }


            return context.respond(responseBuilder.toString());
        };
    }

    @Override
    public boolean isUsableInGuilds() {
        return true;
    }

    @Override
    public boolean isUsableInDMs() {
        return true;
    }

    @Nullable
    @Override
    public Command getSubCommand(@NonNull String subCommand) {
        return null;
    }

    @Override
    public int getHelpPagePosition() {
        return super.getHelpPagePosition();
    }

    @Override
    public boolean isNsfw() {
        return false;
    }

    private final Ratelimit ratelimit = RatelimitFactory.getRatelimit(RatelimitType.GUILD, List.of(Bandwidth.simple(4, Duration.ofMinutes(3))));
    @NonNull
    @Override
    public Ratelimit getRatelimit() {
        // NOTICE: the ratelimit instance should not get created new for every call of this function!
        return ratelimit;
    }

    @Nullable
    @Override
    public icu.taminaminam.spideybot.utils.Permission getRequiredPermissions() {
        return null;
    }

    @NonNull
    @Override
    public PermissionSet getPermissionsNeededByBot() {
        return PermissionSet.of(Permission.SEND_MESSAGES);
    }
}
