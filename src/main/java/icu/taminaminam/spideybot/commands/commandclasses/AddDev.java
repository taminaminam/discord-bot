package icu.taminaminam.spideybot.commands.commandclasses;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFieldData;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import icu.taminaminam.spideybot.commands.Command;
import icu.taminaminam.spideybot.commands.PermissionManager;
import icu.taminaminam.spideybot.data.DBDev;
import icu.taminaminam.spideybot.data.DBUser;
import icu.taminaminam.spideybot.data.DataHandler;
import icu.taminaminam.spideybot.main.BotMain;
import icu.taminaminam.spideybot.utils.BotUtils;
import icu.taminaminam.spideybot.utils.exceptions.BotException;
import icu.taminaminam.spideybot.utils.ratelimits.Ratelimit;
import icu.taminaminam.spideybot.utils.ratelimits.RatelimitFactory;
import icu.taminaminam.spideybot.utils.ratelimits.RatelimitType;
import io.github.bucket4j.Bandwidth;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static icu.taminaminam.spideybot.utils.BotUtils.getLanguageString;

public class AddDev extends Command {
    public AddDev(){
        super();
    }

    @NonNull
    @Override
    public String getName() {
        return "addDev";
    }

    /**
     * @return All the aliases of the command (does not include the name returned by {@link #getName()})
     */
    @NonNull
    @Override
    public String[] getAliases() {
        return new String[]{"addOwner"};
    }

    @NonNull
    @Override
    public Category getCategory() {
        return Category.BOTMOD;
    }

    @NonNull
    @Override
    public CommandExecutor getExecutor() {
        //TODO: add behaviour

        return (context, language, prefix, args) -> {
            Flux<DBDev> devFlux = DataHandler.getAllDevs();

            Mono<List<DBDev>> devListMono = devFlux.collectList();

            List<DBDev> devList = new ArrayList<>();
            devListMono.subscribe(devList::addAll);

            devList.add(DBDev.defaultDev);

            List<String> devStrings = new ArrayList<>();
            for (DBDev dev:
                 devList) {
                //User user = context.getClient().getUserById(dev.getId()).block();
                User user = null;   //TODO: fix this.

                devStrings.add(getLanguageString(language, "defaultresponses.developers.rowtext", Optional.ofNullable(user != null ? user.getUsername() : null).orElse(dev.getId().asString()), dev.isOwner() ? "Yes" : "No", dev.getDevRole(), dev.getStatus()));
            }

            String stringDevList = String.join("\n", devStrings);
            if (stringDevList.equals("") || stringDevList == null) {
                stringDevList = "empty";
            }
            return context.respond(
                    EmbedData.builder()
                            .title(getLanguageString(language, "defaultresponses.command.todo.title"))
                            .description(getLanguageString(language, "defaultresponses.command.todo.description"))
                            .addField(EmbedFieldData.builder()
                                    .name(getLanguageString(language, "defaultresponses.developers.title"))
                                    .value(getLanguageString(language, "defaultresponses.developers.description", stringDevList))
                                    .inline(false)
                                    .build()
                            )
                            .color(BotUtils.BOT_COLOR.getRGB())
                            .build()
            );
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
        //add add-dev permission (should automatically be given to all Owners
        return null;
    }

    @NonNull
    @Override
    public PermissionSet getPermissionsNeededByBot() {
        return PermissionSet.of(Permission.SEND_MESSAGES);
    }

}
