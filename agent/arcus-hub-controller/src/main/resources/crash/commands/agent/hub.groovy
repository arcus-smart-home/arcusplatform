import org.crsh.cli.Command

import com.iris.bootstrap.ServiceLocator;
import com.iris.agent.controller.hub.HubController

@Usage("Iris agent hub control")
class hub {
   @Command
   Object backup() {
      return this.controller.doBackup().absolutePath
   }

   @Command
   Object restore(@Argument String path) {
      return this.controller.doRestore(path)
   }
      
   HubController getController() {
      HubController controller = ServiceLocator.getInstance(HubController.class)
      if (controller == null) {
         throw new RuntimeException("no hub controller");
      }

      return controller;
   }
}
