package icu.taminaminam.spideybot.commands;

import icu.taminaminam.spideybot.commands.commandclasses.AddDev;
import icu.taminaminam.spideybot.commands.commandclasses.ExampleCommandWithOwnClass;
import icu.taminaminam.spideybot.commands.commandclasses.Ping;
import icu.taminaminam.spideybot.data.DataHandler;
import icu.taminaminam.spideybot.utils.exceptions.BotException;
import icu.taminaminam.spideybot.main.BotMain;
import icu.taminaminam.spideybot.utils.BotUtils;
import discord4j.common.GitProperties;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.util.EntityUtil;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFieldData;
import discord4j.discordjson.json.MessageEditRequest;
import discord4j.rest.util.Permission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static icu.taminaminam.spideybot.utils.BotUtils.getLanguageString;

public class Commands {
	
	private Commands(){}
	
	private static final Logger logger = LogManager.getLogger("Commands");
	
	/**
	 * The {@link Map} in which all {@link Command}s are stored. Every alias maps to the command.
	 */
	static final Map<String, Command> commands = new HashMap<>();
	
	/**
	 * All commands should get registered in here. If any command is registered in another class (e.g. a music bot class
	 * with which has its own function to register all commands) it should get called in this function.
	 */
	public static void registerCommands(){
		logger.info("Registering all commands");
		
		new ExampleCommandWithOwnClass().register();
		new AddDev().register();
		new Ping().register();
		
		Command.builder()
				.setName("help")
				.setUsableInDMs(true)
				.setExecutor((context, language, prefix, args) -> {
					if(!args.isEmpty()){
						Command command = Commands.getCommand(args.get(0));
						if(command != null){
							String commandName = command.getName();
							List<String> commandTree = new ArrayList<>();
							commandTree.add(command.getName());
							for(int i = 1; i < args.size(); i++){
								Command cmd = command.getSubCommand(args.get(i));
								if(cmd == null) break;
								commandTree.add(cmd.getName());
								command = cmd;
							}
							return context.respond(EmbedData.builder()
									.title(getLanguageString(language, "help.command.title", commandName))
									.description(getLanguageString(language, "help." + String.join(".", commandTree) + ".detailed", prefix))
									.build()
							);
						}
					}
					Command.Category category = BotUtils.getHelpPage(language, args);
					AtomicInteger helpPage = new AtomicInteger(category.getHelpPage());
					return context.respond(BotUtils.getHelpEmbedData(language, prefix, category))
							.flatMap(messageData -> Mono.when(
									context.getChannel().getRestMessage(Snowflake.of(messageData.id())).createReaction(EntityUtil.getEmojiString(BotUtils.EMOJI_ARROW_LEFT))
											.then(context.getChannel().getRestMessage(Snowflake.of(messageData.id())).createReaction(EntityUtil.getEmojiString(BotUtils.EMOJI_ARROW_RIGHT))),
									context.getClient().on(ReactionAddEvent.class)
											.filter(ev -> ev.getMessageId().asString().equals(messageData.id()))
											.filter(ev -> ev.getUserId().equals(context.getAuthor().getId()))
											.filter(ev -> ev.getEmoji().equals(BotUtils.EMOJI_ARROW_LEFT) || ev.getEmoji().equals(BotUtils.EMOJI_ARROW_RIGHT))
											.timeout(
													Duration.ofMinutes(2),
													Mono.when(
															context.getChannel().getRestMessage(Snowflake.of(messageData.id())).deleteOwnReaction(EntityUtil.getEmojiString(BotUtils.EMOJI_ARROW_LEFT)),
															context.getChannel().getRestMessage(Snowflake.of(messageData.id())).deleteOwnReaction(EntityUtil.getEmojiString(BotUtils.EMOJI_ARROW_RIGHT))
													).then(Mono.empty())
											)
											.flatMap(ev -> {
												if(ev.getEmoji().equals(BotUtils.EMOJI_ARROW_LEFT)) helpPage.decrementAndGet();
												else if(ev.getEmoji().equals(BotUtils.EMOJI_ARROW_RIGHT)) helpPage.incrementAndGet();
												helpPage.set(BotUtils.clamp(1, helpPage.get(), Command.Category.values().length));
												Command.Category newCategory = Command.Category.getCategoryByHelpPage(helpPage.get());
												return Mono.when(
														context.isPrivateMessage() ? Mono.empty() : context.getChannel().getRestMessage(Snowflake.of(messageData.id())).deleteUserReaction(EntityUtil.getEmojiString(ev.getEmoji()), ev.getUserId()),
														context.getChannel().getRestMessage(Snowflake.of(messageData.id())).edit(MessageEditRequest.builder()
																.embed(BotUtils.getHelpEmbedData(language, prefix, Objects.requireNonNull(newCategory)))
																.build()
														)
												);
											})
							));
				})
				.build().register();
		
		Command.collectionBuilder()
				.setName("prefix")
				.setUsableInDMs(true)
				.setCategory(Command.Category.GENERAL)
				.addSubCommand(
						Command.builder()
								.setName("get")
								.setUsableInDMs(true)
								.setExecutor((context, language, prefix, args) -> context.respond(EmbedData.builder()
										.title(getLanguageString(language, "command.prefix.get.title"))
										.description(getLanguageString(language, "command.prefix.get.description", prefix))
										.color(BotUtils.COLOR_LIGHT_GREEN.getRGB())
										.build()
								))
								.build()
				)
				.addSubCommand(
						Command.builder()
								.setName("set")
								.setRequiredPermissions("command.prefix.set", Permission.ADMINISTRATOR)
								.setUsableInDMs(true)
								.setExecutor((context, language, prefix, args) -> {
									if(args.isEmpty()){
										return Mono.error(BotException.invalidArgument("command.prefix.set.missingArgument"));
									}else{
										String newPrefix = args.getRemaining();
										if(newPrefix.chars().anyMatch(Character::isWhitespace)) return Mono.error(BotException.invalidArgument("command.prefix.set.containsWhitespace"));
										Mono<Void> mono;
										if(context.isGuildMessage()){
											Snowflake guildId = context.getGuildId().orElseThrow();
											mono = DataHandler.setGuildPrefix(guildId, newPrefix)
													.then(Mono.fromRunnable(()->BotUtils.setGuildPrefix(guildId, newPrefix)));
										}else{
											Snowflake userId = context.getAuthor().getId();
											mono = DataHandler.setUserPrefix(userId, newPrefix)
													.then(Mono.fromRunnable(()->BotUtils.setUserPrefix(userId, newPrefix)));
										}
										return mono.then(context.respond(EmbedData.builder()
												.title(getLanguageString(language, "command.prefix.set.title"))
												.description(getLanguageString(language, "command.prefix.set.description", prefix, newPrefix))
												.color(BotUtils.COLOR_LIGHT_GREEN.getRGB())
												.build()
										));
									}
								})
								.build()
				)
				.setUnknownSubCommandHandler(
						Command.builder()
								.setUsableInDMs(true)
								.setExecutor((context, language, prefix, args) -> Mono.error(BotException.invalidArgument("command.prefix.invalidArgs", prefix)))
								.build()
				)
				.build().register();
		
		Command.collectionBuilder()
				.setName("language")
				.setUsableInDMs(true)
				.setCategory(Command.Category.GENERAL)
				.addSubCommand(
						Command.builder()
								.setName("get")
								.setUsableInDMs(true)
								.setExecutor((context, language, prefix, args) -> context.respond(EmbedData.builder()
										.title(getLanguageString(language, "command.language.get.title"))
										.description(getLanguageString(language, "command.language.get.description", language))
										.color(BotUtils.COLOR_LIGHT_GREEN.getRGB())
										.build()
								))
								.build()
				)
				.addSubCommand(
						Command.builder()
								.setName("set")
								.setRequiredPermissions("command.language.set", Permission.ADMINISTRATOR)
								.setUsableInDMs(true)
								.setExecutor((context, language, prefix, args) -> {
									if(args.isEmpty()){
										return Mono.error(BotException.invalidArgument("command.language.set.missingArgument"));
									}else{
										String newLanguage = args.getRemaining();
										if(!BotUtils.getAvailableLanguages().contains(newLanguage))
											return Mono.error(BotException.invalidArgument("command.language.set.doesNotExist", newLanguage, prefix));
										Mono<Void> mono;
										if(context.isGuildMessage()){
											Snowflake guildId = context.getGuildId().orElseThrow();
											mono = DataHandler.setGuildLanguage(guildId, newLanguage)
													.then(Mono.fromRunnable(()->BotUtils.setGuildLanguage(guildId, newLanguage)));
										}else{
											Snowflake userId = context.getAuthor().getId();
											mono = DataHandler.setUserLanguage(userId, newLanguage)
													.then(Mono.fromRunnable(()->BotUtils.setUserLanguage(userId, newLanguage)));
										}
										return mono.then(context.respond(EmbedData.builder()
												.title(getLanguageString(newLanguage, "command.language.set.title"))
												.description(getLanguageString(newLanguage, "command.language.set.description", language, newLanguage))
												.color(BotUtils.COLOR_LIGHT_GREEN.getRGB())
												.build()
										));
									}
								})
								.build()
				)
				.addSubCommand(Command.builder()
						.setName("list")
						.setUsableInDMs(true)
						.setExecutor((context, language, prefix, args) -> context.respond(EmbedData.builder()
								.title(getLanguageString(language, "command.language.list.title"))
								.description(
										getLanguageString(language, "command.language.list.description",
										BotUtils.getAvailableLanguages().stream().map(lang -> "`" + lang + "`").collect(Collectors.joining(", "))
								))
								.color(BotUtils.BOT_COLOR.getRGB())
								.build()
						))
						.build()
				)
				.setUnknownSubCommandHandler(
						Command.builder()
								.setUsableInDMs(true)
								.setExecutor((context, language, prefix, args) -> Mono.error(BotException.invalidArgument("command.language.invalidArgs", prefix)))
								.build()
				)
				.build().register();
		
		Command.builder()
				.setName("info")
				.setUsableInDMs(true)
				.setCategory(Command.Category.GENERAL)
				.setExecutor((context, language, prefix, args) -> context.respond(EmbedData.builder()
						.title(getLanguageString(language, "command.info.title"))
						.description(getLanguageString(language, "command.info.general"))
						.addField(EmbedFieldData.builder()
								.name(getLanguageString(language, "command.info.version.title"))
								.value(getLanguageString(language, "command.info.version.description", BotMain.CURRENT_VERSION, BotMain.BOTENV))
								.inline(false)
								.build()
						)
						.addField(EmbedFieldData.builder()
								.name(getLanguageString(language, "command.info.libraries.title"))
								.value(getLanguageString(language, "command.info.libraries.description",
										System.getProperty("java.version"),
										GitProperties.getProperties().getProperty(GitProperties.APPLICATION_VERSION))
								)
								.inline(false)
								.build()
						)
						.color(BotUtils.BOT_COLOR.getRGB())
						.build()
				))
				.build().register();
	}
	
	/**
	 * @param name The name of the command to find
	 * @return The {@link Command} with the specified name
	 */
	@Nullable
	public static Command getCommand(@NonNull String name){
		return commands.get(name.toLowerCase());
	}
	
	/**
	 * @return A {@link Stream} of all commands without any duplicates
	 */
	@NonNull
	public static Stream<Command> getCommands(){
		return commands.values().stream()
				.distinct();
	}
	
	/**
	 * @param category The {@link Command.Category} of which the {@link Command}s should get returned
	 * @return A {@link Stream} with all {@link Command}s in the specified {@link Command.Category} without duplicates
	 */
	@NonNull
	public static Stream<Command> getCommands(@NonNull Command.Category category){
		return commands.values().stream()
				.filter(cmd -> cmd.getCategory() == category)
				.distinct();
	}
	
}
