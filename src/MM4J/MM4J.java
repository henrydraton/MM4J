/*
Macromod for Java
     
Copyright (C) 2018 Henry Jonathan Draton

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

 1. The origin of this software must not be misrepresented; you must not
    claim that you wrote the original software. If you use this software
    in a product, an acknowledgment in the product documentation would be
    appreciated but is not required.
 2. Altered source versions must be plainly marked as such, and must not be
    misrepresented as being the original software.
 3. This notice may not be removed or altered from any source distribution.
 */

package MM4J;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.UUID;

public class MM4J {
	
	File       directory;
	Path       access_file, script_file;
	ByteBuffer access_buffer;

	public MM4J (String profile) {
		
		if (profile == null)
			throw new IllegalArgumentException("profile directory must be specified");
		if (profile.equals(""))
			throw new IllegalArgumentException("profile directory must be specified");
		
		directory = new File( profile , "liteconfig");
		directory = new File(directory, "common"    );
		directory = new File(directory, "macros"    );
		
		access_file = new File(directory, "access.lck").toPath();
		script_file = new File(directory, "script.txt").toPath();

		access_buffer = ByteBuffer.allocateDirect(4);

	}
	
	public Scanner get (Path results) throws IOException {
		if (results == null) throw new NullPointerException();
		synchronized (results) {
			return new Scanner(FileChannel.open(
					results,
					StandardOpenOption.CREATE,
					StandardOpenOption.DELETE_ON_CLOSE,
					StandardOpenOption.READ,
					StandardOpenOption.WRITE
				));
		}
	}
	
	public synchronized Path run (String code) {

		/******************/
		/* INITIALIZATION */
		/******************/

		if (code == null)
			throw new IllegalArgumentException("source code must be specified");
		if (code.equals(""))
			throw new IllegalArgumentException("source code must be specified");
		
		/*****************************/
		/* SCRIPT PASS OFF/EXECUTION */
		/*****************************/

		try (FileChannel access_channel = FileChannel.open(access_file, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			while (true) {
				try (FileLock access_lock = access_channel.lock()) {
					int access_token = 0;
					try {					
						access_buffer.clear();
						access_channel.read(access_buffer);
						access_buffer.position(0);
						access_token = access_buffer.getInt();
					} catch (BufferUnderflowException exception) {
						// NOP
					}
					if ((access_token < 0)||(access_token > 100))
						 access_token = 0;
					String result_name = null;
					Path   result_file = null;
					try {
						result_name = UUID.randomUUID().toString() + ".txt";
						result_file = new File(new File(directory, "logs"), result_name).toPath();
					} catch (Throwable exception) {
						throw new IllegalStateException("failure to create name for the result file", exception);
					}
					try (FileChannel result_channel = FileChannel.open(
							result_file,
							StandardOpenOption.CREATE,
							// StandardOpenOption.DELETE_ON_CLOSE,
							StandardOpenOption.READ,
							StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.WRITE
						)) {
						result_channel.force(false);
					} catch (Throwable exception) {
						throw new IllegalStateException("failure to open the result file", exception);
					}	
					try (FileChannel script_channel = FileChannel.open(
							script_file,
							StandardOpenOption.CREATE,
							StandardOpenOption.READ,
							StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.WRITE
						)) {
						try {
							access_buffer.clear();
							access_buffer.putInt((access_token == 100) ? 0 : (access_token + 1));
							access_buffer.position(0);
							access_channel.position(0);
							access_channel.write(access_buffer);
							access_channel.force(false);
						} catch (Throwable exception) {
							throw new IllegalStateException("failure to write access token to the access file", exception);
						}							
						try {
							ByteBuffer script_buffer = ByteBuffer.wrap((
								  "if(%@#token%==" + access_token + ");\r\n"
								+ "    stop;\r\n"
								+ "    endif;\r\n"
								+ "@#token=" + access_token + ";\r\n"
								+ "&out=\"" + result_name + "\";\r\n"
								+ code).getBytes());
									   script_buffer.position(0);
							script_channel.write(script_buffer);
							script_channel.force(false);
						} catch (Throwable exception) {
							throw new IllegalStateException("failure to write script to the script file", exception);
						}
					}
					return new File(new File(directory, "logs"), result_name).toPath();
				} catch (FileLockInterruptionException exception) {
					continue;
				}
			}
		} catch (Throwable exception) {
			throw new IllegalStateException("failure to execute macromod script", exception);
		}
		
	}

}
