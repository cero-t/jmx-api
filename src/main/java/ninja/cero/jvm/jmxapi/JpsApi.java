package ninja.cero.jvm.jmxapi;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class JpsApi {
    @RequestMapping("/jps")
    public List<Jps> jps() {
        List<Jps> jpsList = new ArrayList<Jps>();

        List<VirtualMachineDescriptor> vmList = VirtualMachine.list();
        for (VirtualMachineDescriptor vm : vmList) {
            Jps jps = new Jps();
            jps.name = vm.displayName();
            jps.pid = vm.id();
            jps.simpleName = toSimpleName(jps.name);
            jpsList.add(jps);
        }

        return jpsList;
    }

    protected String toSimpleName(String name) {
        String[] values = name.split(" ");
        if (values.length == 0) {
            return "";
        }

        values = values[0].split("\\.");
        if (values.length == 0) {
            return "";
        }

        return values[values.length - 1];
    }

    static class Jps {
        public String pid;
        public String name;
        public String simpleName;
    }
}
