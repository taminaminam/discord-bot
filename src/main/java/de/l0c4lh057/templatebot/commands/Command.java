package de.l0c4lh057.templatebot.commands;

import de.l0c4lh057.templatebot.commands.exceptions.*;
import de.l0c4lh057.templatebot.utils.BotUtils;
import de.l0c4lh057.templatebot.utils.ratelimits.NoRatelimit;
import de.l0c4lh057.templatebot.utils.ratelimits.Ratelimit;
import de.l0c4lh057.templatebot.utils.ratelimits.RatelimitFactory;
import de.l0c4lh057.templatebot.utils.ratelimits.RatelimitType;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.EmbedData;
import discord4j.rest.util.PermissionSet;
import io.github.bucket4j.Bandwidth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.*;

import static de.l0c4lh057.templatebot.utils.BotUtils.getLanguageString;

public class Command {
	
	private static final Logger logger = LogManager.getLogger("Command");
	
	@NonNull private final String name;
	@NonNull private final String[] aliases;
	@NonNull private final Category category;
	@NonNull private final CommandExecutor executor;
	private final boolean usableInGuilds;
	private final boolean usableInDMs;
	@NonNull private final Map<String, Command> subCommands;
	private final int helpPagePosition;
	private final boolean nsfw;
	@NonNull private final Ratelimit ratelimit;
	private final boolean requiresBotOwner;
	private final boolean requiresGuildOwner;
	@Nullable private final Permission requiredPermissions;
	
	private Command(CommandBuilder builder){
		this.name = builder.name;
		this.aliases = builder.aliases;
		this.category = builder.category;
		this.executor = builder.executor;
		this.usableInGuilds = builder.usableInGuilds;
		this.usableInDMs = builder.usableInDMs;
		this.subCommands = Collections.emptyMap();
		this.helpPagePosition = builder.helpPagePosition;
		this.nsfw = builder.nsfw;
		this.ratelimit = builder.ratelimit;
		this.requiresBotOwner = builder.requiresBotOwner;
		this.requiresGuildOwner = builder.requiresGuildOwner;
		this.requiredPermissions = builder.requiredPermissions;
	}
	
	private Command(CommandCollectionBuilder builder){
		this.name = builder.name;
		this.aliases = builder.aliases;
		this.category = builder.category;
		this.usableInGuilds = builder.usableInGuilds;
		this.usableInDMs = builder.usableInDMs;
		this.subCommands = builder.subCommands;
		this.helpPagePosition = builder.helpPagePosition;
		this.nsfw = builder.nsfw;
		this.ratelimit = builder.ratelimit;
		this.requiresBotOwner = builder.requiresBotOwner;
		this.requiresGuildOwner = builder.requiresGuildOwner;
		this.requiredPermissions = builder.requiredPermissions;
		Command unknownSubCommandHandler = builder.unknownSubCommandHandler;
		this.executor = (event, language, prefix, args) -> {
			if(!args.isEmpty()){
				Command command = subCommands.get(args.get(0).toLowerCase());
				if(command != null){
					return command.execute(event, language, prefix, args.subList(1, args.size()), false);
				}
			}
			return unknownSubCommandHandler.execute(event, language, prefix, args, false);
		};
	}
	
	/**
	 * @return The name of the command
	 */
	@NonNull public String getName(){ return name; }
	
	/**
	 * @return All the aliases of the command
	 */
	@NonNull public String[] getAliases(){ return aliases; }
	
	/**
	 * @return The {@link Category} the command is in
	 */
	@NonNull public Category getCategory(){ return category; }
	
	/**
	 * @return The {@link CommandExecutor} of this command
	 */
	@NonNull public CommandExecutor getExecutor(){ return executor; }
	
	/**
	 * @return Whether the command can be executed in guilds
	 */
	public boolean isUsableInGuilds(){ return usableInGuilds; }
	
	/**
	 * @return Whether this command can be executed in DMs
	 */
	public boolean isUsableInDMs(){ return usableInDMs; }
	
	/**
	 * @param subCommand The name of the sub command
	 * @return The {@link Command} with the passed value as name or alias or null if it does not exist
	 */
	@Nullable public Command getSubCommand(@NonNull String subCommand){ return subCommands.get(subCommand.toLowerCase()); }
	
	/**
	 * @return The position of the command on the help page
	 */
	public int getHelpPagePosition(){ return helpPagePosition; }
	
	/**
	 * @return Whether this command can only be used in NSFW channels and DMs
	 */
	public boolean isNsfw(){ return nsfw; }
	
	/**
	 * This function checks if the user/guild is rate limited.
	 * <p>
	 * In case of a rate limit this function will just return true,
	 * otherwise it will automatically increase the usage count by one.
	 *
	 * @param guildId   The {@link Snowflake} ID of the guild the command got executed in or null if executed in DMs
	 * @param channelId The {@link Snowflake} ID of the channel the command got executed in
	 * @param userId    The {@link Snowflake} ID of the user who executed the command
	 * @return Whether the guild/member/user/channel is rate limited
	 */
	public boolean isRatelimited(@Nullable Snowflake guildId, @NonNull Snowflake channelId, @NonNull Snowflake userId){
		return ratelimit.isRatelimited(guildId, channelId, userId);
	}
	
	/**
	 * Adds this command with name and aliases as key to {@link Commands#commands}.
	 */
	public void register(){
		if(Commands.getCommand(name) != null || Arrays.stream(aliases).anyMatch(alias -> Commands.getCommand(alias) != null)){
			logger.warn("Command {} is already registered", name);
		}else{
			Commands.commands.put(name.toLowerCase(), this);
			for (String alias : aliases) {
				Commands.commands.put(alias.toLowerCase(), this);
			}
		}
	}
	
	/**
	 * Executes this command. If an error occurs it will automatically get caught and processed.
	 *
	 * @param event    The raw {@link MessageCreateEvent} that cause the call of this command
	 * @param language The language that should be used in responses
	 * @param prefix   The bot prefix
	 * @param args     The list of all arguments passed to this command
	 * @return An empty {@link Mono}.
	 */
	@NonNull public Mono<Void> execute(@NonNull MessageCreateEvent event, @NonNull String language, @NonNull String prefix, @NonNull ArgumentList args){
		return execute(event, language, prefix, args, true);
	}
	
	/**
	 * Executes this command. If an error occurs and {@code handleExceptions} is set to true
	 * it will automatically get caught and processed.
	 *
	 * @param event    The raw {@link MessageCreateEvent} that caused the call of this command
	 * @param language The language that should be used in responses
	 * @param prefix   The bot prefix
	 * @param args     The list of all arguments passed to this command
	 * @return An empty {@link Mono}. It will contain an error if command execution failed and {@code handleExceptions}
	 * is set to false.
	 */
	@NonNull private Mono<Void> execute(@NonNull MessageCreateEvent event, @NonNull String language, @NonNull String prefix, @NonNull ArgumentList args, boolean handleExceptions){
		Snowflake authorId = event.getMessage().getAuthor().map(User::getId).orElseThrow();
		Mono<?> executionMono;
		if(requiresBotOwner && !BotUtils.botOwners.contains(authorId)){
			executionMono = Mono.error(CommandException.notExecutable("exception.requiresbotowner"));
		}else if(event.getGuildId().isPresent() && !usableInGuilds){
			executionMono = Mono.error(CommandException.notExecutable("exception.notexecutableinguilds"));
		}else if(event.getGuildId().isEmpty() && !usableInDMs){
			executionMono = Mono.error(CommandException.notExecutable("exception.notexecutableindms"));
		}else if(isRatelimited(event.getGuildId().orElse(null), event.getMessage().getChannelId(), authorId)){
			executionMono = Mono.error(CommandException.ratelimited("exception.ratelimited"));
		}else{
			executionMono = PermissionManager.checkExecutability(event.getGuildId().orElse(null), authorId, event.getMessage().getChannelId(), requiredPermissions, requiresGuildOwner, nsfw)
					.then(executor.execute(event, language, prefix, args));
		}
		if(handleExceptions) return handleExceptions(executionMono, event, language, name);
		else return executionMono.then();
	}
	
	/**
	 * This function simply catches all the exceptions that could happen on when executing the command.
	 *
	 * @param executionMono The result {@link Mono} got by command execution
	 * @param event         The raw {@link MessageCreateEvent} that caused the call of this command
	 * @param language      The language that should be used in responses
	 * @param commandName   The name of the command
	 * @return An empty {@link Mono}
	 */
	@NonNull private static Mono<Void> handleExceptions(@NonNull Mono<?> executionMono, @NonNull MessageCreateEvent event, @NonNull String language, @NonNull String commandName){
		return executionMono.then()
				.onErrorResume(MissingPermissionsException.class, err -> event.getMessage().getRestChannel()
						.createMessage(EmbedData.builder()
								.title(getLanguageString(language, "exception.missingpermissions.title"))
								.description(err.getErrorMessage(language))
								.color(BotUtils.COLOR_LIGHT_RED.getRGB())
								.build()
						).then()
						.onErrorResume(ex -> Mono.empty())
				)
				.onErrorResume(InvalidArgumentException.class, err -> event.getMessage().getRestChannel()
						.createMessage(EmbedData.builder()
								.title(getLanguageString(language, "exception.invalidargument.title"))
								.description(err.getErrorMessage(language))
								.color(BotUtils.COLOR_LIGHT_RED.getRGB())
								.build()
						).then()
						.onErrorResume(ex -> Mono.empty())
				)
				.onErrorResume(RatelimitedException.class, err -> event.getMessage().getRestChannel()
						.createMessage(EmbedData.builder()
								.title(getLanguageString(language, "exception.ratelimited.title"))
								.description(err.getErrorMessage(language))
								.color(BotUtils.COLOR_LIGHT_RED.getRGB())
								.build()
						).then()
						.onErrorResume(ex -> Mono.empty())
				)
				.onErrorResume(NotExecutableException.class, err -> event.getMessage().getRestChannel()
						.createMessage(EmbedData.builder()
								.title(getLanguageString(language, "exception.notexecutable.title"))
								.description(err.getErrorMessage(language))
								.color(BotUtils.COLOR_LIGHT_RED.getRGB())
								.build()
						).then()
						.onErrorResume(ex -> Mono.empty())
				)
				.onErrorResume(err -> {
					logger.error("Unexpected exception when executing command " + commandName, err);
					return event.getMessage().getRestChannel()
							.createMessage(EmbedData.builder()
									.title(getLanguageString(language, "exception.unknown.title"))
									.description(getLanguageString(language, "exception.unknown"))
									.color(BotUtils.COLOR_DARK_RED.getRGB())
									.build()
							).then()
							.onErrorResume(ex -> Mono.empty());
				});
	}
	
	/**
	 * @return A new {@link CommandBuilder} instance
	 */
	@NonNull
	public static CommandBuilder builder(){
		return new CommandBuilder();
	}
	
	/**
	 * A command collection is a {@link Command} that has a list of sub commands. The first passed argument is used to
	 * get the sub command which will then be executed with all initial arguments except the first one which is just the
	 * command name itself.
	 *
	 * @return A new {@link CommandCollectionBuilder} instance
	 */
	@NonNull
	public static CommandCollectionBuilder collectionBuilder(){
		return new CommandCollectionBuilder();
	}
	
	@Override
	public boolean equals(@Nullable Object o){
		return (o instanceof Command) && ((Command)o).name.equals(name);
	}
	
	@Override
	public int hashCode(){
		return name.hashCode();
	}
	
	public static class CommandBuilder {
		private String name = "";
		private String[] aliases = {};
		private CommandExecutor executor = null;
		private Category category = Category.GENERAL;
		private boolean usableInGuilds = true;
		private boolean usableInDMs = false;
		private int helpPagePosition = 0;
		private boolean nsfw = false;
		private Ratelimit ratelimit = NoRatelimit.getInstance();
		private boolean requiresBotOwner = false;
		private boolean requiresGuildOwner = false;
		private Permission requiredPermissions = null;
		
		private CommandBuilder(){}
		
		/**
		 * @param name    The name this command should have
		 * @param aliases The list of aliases this command has
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandBuilder setName(@NonNull String name, @NonNull String... aliases){
			this.name = name;
			this.aliases = aliases;
			return this;
		}
		
		/**
		 * It is guaranteed that {@link Message#getAuthor()} of {@link MessageCreateEvent#getMessage()} will never be empty.
		 *
		 * @param executor The action that should get performed when executing the command
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandBuilder setExecutor(@NonNull CommandExecutor executor){
			this.executor = executor;
			return this;
		}
		
		/**
		 * @param category The {@link Category} this command should be listed in
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandBuilder setCategory(@NonNull Category category){
			this.category = category;
			return this;
		}
		
		/**
		 * By default all {@link Command}s on the help page are sorted by their name.
		 * If a custom order is desired this value can be set. All commands have a default of {@code 0} and after sorting
		 * by name they are sorted by this value.
		 *
		 * @param helpPagePosition The position of this command on the help page
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandBuilder setHelpPagePosition(int helpPagePosition){
			this.helpPagePosition = helpPagePosition;
			return this;
		}
		
		/**
		 * @param nsfw Whether this command should only be executable in NSFW channels and DMs
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandBuilder setNsfw(boolean nsfw){
			this.nsfw = nsfw;
			return this;
		}
		
		/**
		 * @param usableInDMs Whether this command can be executed in DMs
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandBuilder setUsableInDMs(boolean usableInDMs){
			this.usableInDMs = usableInDMs;
			return this;
		}
		
		/**
		 * @param usableInGuilds Whether this command can be executed in guilds
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandBuilder setUsableInGuilds(boolean usableInGuilds){
			this.usableInGuilds = usableInGuilds;
			return this;
		}
		
		/**
		 * @param requiresBotOwner
		 * @return
		 */
		@NonNull
		public CommandBuilder setRequiresBotOwner(boolean requiresBotOwner){
			this.requiresBotOwner = requiresBotOwner;
			return this;
		}
		
		/**
		 * @param requiresGuildOwner
		 * @return
		 */
		@NonNull
		public CommandBuilder setRequiresGuildOwner(boolean requiresGuildOwner){
			this.requiresGuildOwner = requiresGuildOwner;
			return this;
		}
		
		/**
		 * @param permissionName
		 * @param defaultPermissions
		 * @return
		 */
		@NonNull
		public CommandBuilder setRequiredPermissions(@NonNull String permissionName, @NonNull discord4j.rest.util.Permission... defaultPermissions){
			this.requiredPermissions = new Permission(permissionName, defaultPermissions);
			return this;
		}
		
		/**
		 *
		 * @param type
		 * @param bandwidths
		 * @return
		 */
		@NonNull
		public CommandBuilder setRatelimit(@NonNull RatelimitType type, @NonNull Bandwidth... bandwidths){
			this.ratelimit = RatelimitFactory.getRatelimit(type, List.of(bandwidths));
			return this;
		}
		
		@NonNull
		private CommandBuilder setRatelimit(@NonNull Ratelimit ratelimit){
			this.ratelimit = ratelimit;
			return this;
		}
		
		/**
		 * Creates a command instance with the values defined in this builder.
		 *
		 * @return The built {@link Command}
		 */
		@NonNull
		public Command build(){
			return new Command(this);
		}
	}
	
	public static class CommandCollectionBuilder {
		private String name = "";
		private String[] aliases = {};
		private final Map<String, Command> subCommands = new HashMap<>();
		private Command unknownSubCommandHandler = null;
		private Command.Category category = Category.GENERAL;
		private boolean usableInGuilds = true;
		private boolean usableInDMs = false;
		private int helpPagePosition = 0;
		private boolean nsfw = false;
		private final Ratelimit ratelimit = NoRatelimit.getInstance();
		private boolean requiresBotOwner = false;
		private boolean requiresGuildOwner = false;
		private Permission requiredPermissions = null;
		
		private CommandCollectionBuilder(){}
		
		/**
		 * @param name    The name this command should have
		 * @param aliases The list of aliases this command has
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setName(@NonNull String name, @NonNull String... aliases){
			this.name = name;
			this.aliases = aliases;
			return this;
		}
		
		/**
		 * @param category The {@link Category} this command should be listed in
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setCategory(@NonNull Command.Category category){
			this.category = category;
			return this;
		}
		
		/**
		 * By default all {@link Command}s on the help page are sorted by their name.
		 * If a custom order is desired this value can be set. All commands have a default of {@code 0} and after sorting
		 * by name they are sorted by this value.
		 *
		 * @param helpPagePosition The position of this command on the help page
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setHelpPagePosition(int helpPagePosition){
			this.helpPagePosition = helpPagePosition;
			return this;
		}
		
		/**
		 * @param nsfw Whether this command should only be executable in NSFW channels and DMs
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setNsfw(boolean nsfw){
			this.nsfw = nsfw;
			return this;
		}
		
		/**
		 * @param usableInDMs Whether this command can be executed in DMs
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setUsableInDMs(boolean usableInDMs){
			this.usableInDMs = usableInDMs;
			return this;
		}
		
		/**
		 * @param usableInGuilds Whether this command can be executed in guilds
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setUsableInGuilds(boolean usableInGuilds){
			this.usableInGuilds = usableInGuilds;
			return this;
		}
		
		/**
		 * If this value is set to true only bot owners can execute the command.
		 * Bot owners are stored in {@link BotUtils#botOwners}.
		 *
		 * @param requiresBotOwner Whether only bot owners should be able to execuute the command
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setRequiresBotOwner(boolean requiresBotOwner){
			this.requiresBotOwner = requiresBotOwner;
			return this;
		}
		
		/**
		 * If this value is set to true only guild owners can execute the command.
		 *
		 * @param requiresGuildOwner Whether only guild owners should be able to execute this command
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setRequiresGuildOwner(boolean requiresGuildOwner){
			this.requiresGuildOwner = requiresGuildOwner;
			return this;
		}
		
		/**
		 *
		 * @param permissionName
		 * @param defaultPermissions
		 * @return
		 */
		@NonNull
		public CommandCollectionBuilder setRequiredPermissions(@NonNull String permissionName, @NonNull discord4j.rest.util.Permission... defaultPermissions){
			this.requiredPermissions = new Permission(permissionName, defaultPermissions);
			return this;
		}
		
		/**
		 * Adds a sub command to this command.
		 * <p>
		 * The first argument which matches with any name or alias of the command will be removed from the argument list.
		 * <p>
		 * Neither {@link Category} nor {@code helpPagePosition} property will have an effect on sub commands.
		 *
		 * @param command The sub command
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder addSubCommand(@NonNull Command command){
			if(subCommands.get(command.getName()) != null || Arrays.stream(command.getAliases()).anyMatch(alias -> subCommands.get(alias) != null)){
				logger.warn("Sub command {} of command {} already got registered", command.getName(), name);
			}else{
				subCommands.put(command.getName().toLowerCase(), command);
				Arrays.stream(command.getAliases()).forEach(alias -> subCommands.put(alias.toLowerCase(), command));
			}
			return this;
		}
		
		/**
		 * Sets a {@link Command} as the handler for all command executions that could not find any sub command with
		 * matching name.
		 * <p>
		 * Other than for sub commands the first argument (if present) will not get removed.
		 * <p>
		 * Neither {@link Category} not {@code helpPagePosition} nor {@code name} property will have an effect.
		 *
		 * @param unknownSubCommandHandler The command that should get executed when no sub command matches
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		@NonNull
		public CommandCollectionBuilder setUnknownSubCommandHandler(@NonNull Command unknownSubCommandHandler){
			this.unknownSubCommandHandler = unknownSubCommandHandler;
			return this;
		}
		
		/**
		 * Creates a command instance with the values defined in this builder.
		 *
		 * @return The built {@link Command}
		 */
		@NonNull
		public Command build(){
			if(unknownSubCommandHandler == null){
				logger.warn("No unknown subcommand handler set for command collection {}", name);
				unknownSubCommandHandler = builder().setExecutor((event, language, prefix, args) -> Mono.empty()).build();
			}
			return new Command(this);
		}
	}
	
	public interface CommandExecutor {
		/**
		 *
		 * @param event
		 * @param language
		 * @param prefix
		 * @param args
		 * @return
		 */
		@NonNull
		Mono<?> execute(@NonNull MessageCreateEvent event, @NonNull String language, @NonNull String prefix, @NonNull ArgumentList args);
	}
	
	public enum Category {
		GENERAL(1, "commandcategory.general")
		;
		private final int helpPage;
		private final String nameKey;
		Category(int helpPage, @NonNull String nameKey){
			this.helpPage = helpPage;
			this.nameKey = nameKey;
		}
		/**
		 *
		 * @return
		 */
		public int getHelpPage() { return helpPage; }
		/**
		 *
		 * @param lang
		 * @return
		 */
		@NonNull
		public String getName(@NonNull String lang){ return BotUtils.getLanguageString(lang, nameKey); }
		/**
		 *
		 * @param helpPage
		 * @return
		 */
		@Nullable
		public static Category getCategoryByHelpPage(int helpPage){
			Category[] categories = Category.values();
			for (Category category : categories) {
				if (category.helpPage == helpPage) return category;
			}
			logger.warn("The requested category with helpPage {} does not exist", helpPage);
			return null;
		}
		/**
		 *
		 * @param lang
		 * @param name
		 * @return
		 */
		@Nullable
		public static Category getCategoryByName(@NonNull String lang, @NonNull String name){
			for (Category category : Category.values()) {
				if (category.getName(lang).equalsIgnoreCase(name)) return category;
			}
			return null;
		}
	}
	
	public static class Permission {
		private final String permissionName;
		private final PermissionSet defaultPermissions;
		private Permission(@NonNull String permissionName, @NonNull discord4j.rest.util.Permission... defaultPermissions){
			this.permissionName = permissionName;
			this.defaultPermissions = PermissionSet.of(defaultPermissions);
		}
		/**
		 * @return
		 */
		@NonNull public String getPermissionName(){ return permissionName; }
		/**
		 * @return
		 */
		@NonNull public PermissionSet getDefaultPermissions(){ return defaultPermissions; }
	}
	
}