package icu.taminaminam.spideybot.utils;

import icu.taminaminam.spideybot.commands.Command;
import icu.taminaminam.spideybot.commands.Commands;
import icu.taminaminam.spideybot.data.DBGuild;
import icu.taminaminam.spideybot.data.DBUser;
import icu.taminaminam.spideybot.data.DataHandler;
import discord4j.common.util.Snowflake;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFooterData;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.io.File;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BotUtils {
	
	private static final Logger logger = LogManager.getLogger("BotUtils");
	
	private BotUtils(){}
	
	/* TODO: set the desired values */
	/**
	 * The {@link List} of all users that should be considered an owner of this bot
	 */
	static long taminaminam_id = 586658831195439134L;
	public static final List<Snowflake> botOwners = Collections.singletonList(
			//Snowflake.of(226677096091484160L)
			Snowflake.of(taminaminam_id)
	);
	/**
	 * The {@link Color} used in several {@link Command}s as embed color
	 */
	public static final Color BOT_COLOR = Color.of(0xA71814);
	/**
	 * The prefix that should get used by default if not changed by any user
	 */
	public static final String DEFAULT_PREFIX = "spidey";
	
	public static final Color COLOR_DARK_RED = Color.of(0xFF0000);
	public static final Color COLOR_LIGHT_RED = Color.of(0xFF5246);
	public static final Color COLOR_LIGHT_GREEN = Color.of(0x4BB543);
	
	public static final ReactionEmoji EMOJI_ARROW_LEFT = ReactionEmoji.unicode("\u2B05");
	public static final ReactionEmoji EMOJI_ARROW_RIGHT = ReactionEmoji.unicode("\u27A1");
	public static final ReactionEmoji EMOJI_X = ReactionEmoji.unicode("\u274C");
	public static final ReactionEmoji EMOJI_CHECKMARK = ReactionEmoji.unicode("\u2705");
	
	private static final Map<Long, String> guildPrefixes = new WeakHashMap<>();
	@NonNull public static Mono<String> getGuildPrefix(@NonNull Snowflake guildId){
		String prefix = guildPrefixes.get(guildId.asLong());
		if(prefix != null) return Mono.just(prefix);
		return DataHandler.getGuild(guildId)
				.map(DBGuild::getPrefix)
				.doOnNext(pref -> guildPrefixes.put(guildId.asLong(), pref));
	}
	public static void setGuildPrefix(Snowflake guildId, String prefix){
		guildPrefixes.put(guildId.asLong(), prefix);
	}
	private static final Map<Long, String> guildLanguages = new WeakHashMap<>();
	@NonNull public static Mono<String> getGuildLanguage(@NonNull Snowflake guildId){
		String language = guildLanguages.get(guildId.asLong());
		if(language != null) return Mono.just(language);
		return DataHandler.getGuild(guildId)
				.map(DBGuild::getLanguage)
				.doOnNext(lang -> guildLanguages.put(guildId.asLong(), lang));
	}
	public static void setGuildLanguage(Snowflake guildId, String lang){
		guildLanguages.put(guildId.asLong(), lang);
	}
	
	private static final Map<Long, String> userPrefixes = new WeakHashMap<>();
	@NonNull public static Mono<String> getUserPrefix(@NonNull Snowflake userId){
		String prefix = userPrefixes.get(userId.asLong());
		if(prefix != null) return Mono.just(prefix);
		return DataHandler.getUser(userId)
				.doOnNext(user -> userLanguages.put(userId.asLong(), user.getLanguage()))
				.map(DBUser::getPrefix)
				.doOnNext(pref -> userPrefixes.put(userId.asLong(), pref));
	}
	public static void setUserPrefix(Snowflake userId, String prefix){
		userPrefixes.put(userId.asLong(), prefix);
	}
	private static final Map<Long, String> userLanguages = new WeakHashMap<>();
	@NonNull public static Mono<String> getUserLanguage(@NonNull Snowflake userId){
		String language = userLanguages.get(userId.asLong());
		if(language != null) return Mono.just(language);
		return DataHandler.getUser(userId)
				.doOnNext(user -> userPrefixes.put(userId.asLong(), user.getPrefix()))
				.map(DBUser::getLanguage)
				.doOnNext(lang -> userLanguages.put(userId.asLong(), lang));
	}
	public static void setUserLanguage(Snowflake userId, String language){
		userLanguages.put(userId.asLong(), language);
	}
	
	@NonNull
	public static <T extends Comparable<T>> T clamp(@NonNull T min, @NonNull T value, @NonNull T max){
		return min.compareTo(value) > 0 ? min : max.compareTo(value) < 0 ? max : value;
	}
	
	/**
	 * Gets the help page by checking the provided arguments for a page number or category name. If no category can be
	 * found by checking the arguments the category with help page 1 is returned.
	 *
	 * @param lang The language in which category names should get checked in
	 * @param args The arguments
	 * @return The category specified in the arguments or the first category
	 */
	@NonNull
	public static Command.Category getHelpPage(@NonNull String lang, @NonNull List<String> args){
		if(args.size() == 0) return Objects.requireNonNull(Command.Category.getCategoryByHelpPage(1));
		try {
			int page = BotUtils.clamp(1, Integer.parseInt(args.get(0)), Command.Category.values().length);
			return Objects.requireNonNull(Command.Category.getCategoryByHelpPage(page));
		} catch (NumberFormatException ignored){}
		Command.Category category = Command.Category.getCategoryByName(lang, String.join(" ", args));
		if(category != null) return category;
		else return Objects.requireNonNull(Command.Category.getCategoryByHelpPage(1));
	}
	
	/**
	 *
	 * @param language
	 * @param prefix
	 * @param category
	 * @return
	 */
	@NonNull
	public static EmbedData getHelpEmbedData(@NonNull String language, @NonNull String prefix, @NonNull Command.Category category){
		return EmbedData.builder()
				.title(getLanguageString(language, "help.category.title", category.getName(language)))
				.description(Commands.getCommands(category)
						.sorted(Comparator.comparing(Command::getName))
						.sorted(Comparator.comparing(Command::getHelpPagePosition))
						.map(command -> getLanguageString(language, "help." + command.getName() + ".short", prefix))
						.collect(Collectors.joining("\n"))
				)
				.footer(EmbedFooterData.builder()
						.text(getLanguageString(language, "help.category.footer", category.getHelpPage(), Command.Category.values().length))
						.build()
				)
				.build();
	}
	
	/**
	 * Initializes the language module of the bot by loading all {@link ResourceBundle}s and {@link Locale}s available.
	 * Also sets the default {@link Locale} to {@link Locale#ENGLISH}.
	 */
	public static void initialize(){
		Locale.setDefault(Locale.ENGLISH);
		try {
			availableLanguages.clear();
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Arrays.stream(Objects.requireNonNull(new File(Objects.requireNonNull(loader.getResource("./")).toURI()).listFiles()))
					.filter(File::isFile)
					.map(File::getName)
					.filter(name -> name.startsWith("strings") && name.endsWith(".properties"))
					.map(name -> name.substring(7, name.length() - 11))
					.forEach(lang -> {
						if(lang.length() == 0) availableLanguages.add(Locale.getDefault().getLanguage());
						else availableLanguages.add(Locale.forLanguageTag(lang).getLanguage());
					});
			loadResourceBundles();
		} catch (URISyntaxException ex) {
			logger.error("Could not list files in resources directory", ex);
			System.exit(-1);
		}
	}
	
	/**
	 * The list of all languages a {@code strings} {@link ResourceBundle} was found for.
	 */
	private static final List<String> availableLanguages = new ArrayList<>();
	private static final Map<String, ResourceBundle> bundles = new HashMap<>();
	private static void loadResourceBundles(){
		availableLanguages.forEach(lang -> bundles.put(lang, ResourceBundle.getBundle("strings", Locale.forLanguageTag(lang))));
	}
	
	/**
	 * Gets the string with the provided {@code key} from the {@code strings} {@link ResourceBundle} and formats it
	 * with the provided arguments using {@link MessageFormat#format(Object)}.
	 *
	 * @param language The language the returned string should be in
	 * @param key      The key for the string in the {@link ResourceBundle}s
	 * @param args     The arguments used to format the string
	 * @return The formatted string
	 */
	@NonNull
	public static String getLanguageString(@NonNull String language, @NonNull String key, @NonNull Object... args){
		if(args.length == 0) return bundles.get(language).getString(key);
		else return new MessageFormat(bundles.get(language).getString(key), Locale.forLanguageTag(language)).format(args);
	}
	
	/**
	 *
	 * @return
	 */
	public static List<String> getAvailableLanguages(){
		return Collections.unmodifiableList(availableLanguages);
	}
	
}
