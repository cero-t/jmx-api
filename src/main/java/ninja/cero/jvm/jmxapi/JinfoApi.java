package ninja.cero.jvm.jmxapi;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class JinfoApi {
    @RequestMapping("/jinfo/{pid}")
    public Jinfo jinfo(@PathVariable String pid) throws IOException, AttachNotSupportedException {
        VirtualMachine vm = VirtualMachine.attach(pid);

        Jinfo jinfo = new Jinfo();
        try {
            jinfo.systemProperties = vm.getSystemProperties();
            jinfo.agentProperties = vm.getAgentProperties();
        } finally {
            vm.detach();
        }

        return jinfo;
    }

    static class Jinfo {
        public Map<Object, Object> systemProperties;
        public Map<Object, Object> agentProperties;
    }
}
