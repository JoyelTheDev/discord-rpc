package net.arikia.dev.drpc;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiscordRPC {

	private static final String DLL_VERSION = "3.4.0";
	private static final String LIB_VERSION = "1.6.2";

	private static boolean loaded = false;

	static {
		loadDLL();
	}

	public static void discordInitialize(String applicationId, DiscordEventHandlers handlers, boolean autoRegister) {
		if (!loaded) return;
		DLL.INSTANCE.Discord_Initialize(applicationId, handlers, autoRegister ? 1 : 0, null);
	}

	public static void discordRegister(String applicationId, String command) {
		if (!loaded) return;
		DLL.INSTANCE.Discord_Register(applicationId, command);
	}

	public static void discordInitialize(String applicationId, DiscordEventHandlers handlers, boolean autoRegister, String steamId) {
		if (!loaded) return;
		DLL.INSTANCE.Discord_Initialize(applicationId, handlers, autoRegister ? 1 : 0, steamId);
	}

	public static void discordRegisterSteam(String applicationId, String steamId) {
		if (!loaded) return;
		DLL.INSTANCE.Discord_RegisterSteamGame(applicationId, steamId);
	}

	public static void discordUpdateEventHandlers(DiscordEventHandlers handlers) {
		if (!loaded) return;
		DLL.INSTANCE.Discord_UpdateHandlers(handlers);
	}

	public static void discordShutdown() {
		if (!loaded) return;
		DLL.INSTANCE.Discord_Shutdown();
	}

	public static void discordRunCallbacks() {
		if (!loaded) return;
		DLL.INSTANCE.Discord_RunCallbacks();
	}

	public static void discordUpdatePresence(DiscordRichPresence presence) {
		if (!loaded) return;
		DLL.INSTANCE.Discord_UpdatePresence(presence);
	}

	public static void discordClearPresence() {
		if (!loaded) return;
		DLL.INSTANCE.Discord_ClearPresence();
	}

	public static void discordRespond(String userId, DiscordReply reply) {
		if (!loaded) return;
		DLL.INSTANCE.Discord_Respond(userId, reply.reply);
	}

	public static boolean isLoaded() {
		return loaded;
	}

	private static void loadDLL() {
		try {
			System.setProperty("jna.nosys", "true");

			String name = System.mapLibraryName("discord-rpc");
			OSUtil osUtil = new OSUtil();
			String dir;

			if (osUtil.isMac()) {
				dir = "darwin";
			} else if (osUtil.isWindows()) {
				String arch = System.getProperty("sun.arch.data.model", "64");
				dir = arch.equals("64") ? "win-x64" : "win-x86";
			} else {
				String arch = System.getProperty("os.arch", "").toLowerCase();
				if (arch.contains("aarch64") || arch.contains("arm64")) {
					System.out.println("Discord RPC: arm64/aarch64 detected — native binary not available. Discord RPC disabled.");
					loaded = false;
					return;
				}
				dir = "linux";
			}

			String finalPath = "/" + dir + "/" + name;
			InputStream in = DiscordRPC.class.getResourceAsStream(finalPath);

			if (in == null) {
				System.out.println("Discord RPC native library not found at " + finalPath + ". Discord RPC disabled.");
				loaded = false;
				return;
			}

			Path tempDir = Files.createTempDirectory("drpc");
			File f = new File(tempDir.toFile(), name);

			try (InputStream input = in; OutputStream out = openOutputStream(f)) {
				copyFile(input, out);
			}

			f.deleteOnExit();
			tempDir.toFile().deleteOnExit();
			System.load(f.getAbsolutePath());
			loaded = true;

		} catch (Throwable t) {
			loaded = false;
			System.out.println("Discord RPC failed to load: " + t.getMessage() + ". Discord RPC disabled.");
		}
	}

	private static void copyFile(final InputStream input, final OutputStream output) throws IOException {
		byte[] buffer = new byte[1024 * 4];
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
	}

	private static FileOutputStream openOutputStream(final File file) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) throw new IOException("File '" + file + "' exists but is a directory");
			if (!file.canWrite()) throw new IOException("File '" + file + "' cannot be written to");
		} else {
			final File parent = file.getParentFile();
			if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
				throw new IOException("Directory '" + parent + "' could not be created");
			}
		}
		return new FileOutputStream(file);
	}

	public enum DiscordReply {
		NO(0), YES(1), IGNORE(2);

		public final int reply;

		DiscordReply(int reply) {
			this.reply = reply;
		}
	}

	private interface DLL extends Library {
		DLL INSTANCE = Native.loadLibrary("discord-rpc", DLL.class);

		void Discord_Initialize(String applicationId, DiscordEventHandlers handlers, int autoRegister, String optionalSteamId);
		void Discord_Register(String applicationId, String command);
		void Discord_RegisterSteamGame(String applicationId, String steamId);
		void Discord_UpdateHandlers(DiscordEventHandlers handlers);
		void Discord_Shutdown();
		void Discord_RunCallbacks();
		void Discord_UpdatePresence(DiscordRichPresence presence);
		void Discord_ClearPresence();
		void Discord_Respond(String userId, int reply);
	}
}
