package One;

import java.nio.file.Path;
import java.util.Scanner;

import MM4J.MM4J;

public class One {

	public static void main (String[] args) {
	
		/*
		 * This method illustrates a convenient and reliable way to use the package.
		 * 
		 *  - Inject code into client by executing run() with the desired instructions.
		 *  - Read in loops and ignore exceptions by retrying after a wait.
		 *  - Make sure that logto is used only once, in bulk, for atomicity.
		 * 
		 */
		
		System.out.println("Instantiating");
		MM4J m = new MM4J("/home/charles/.minecraft-HenryDraton");
			// Replace with your .minecraft directory!
		while (true) {
			System.out.println("Running");
			Path r = m.run(
					  "log(\"Hello world!\");\r\n"
					+ "log(%&out%);\r\n"
					+ "logto(%&out%,\"%&out% says hi!\");\r\n"
				);
			System.out.println("Reading");
			while (true) {
				try {
					try (Scanner s = m.get(r)) {
						System.out.println(s.nextLine());
					}
					break;
				} catch (Throwable exception) {
					exception.printStackTrace();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}
