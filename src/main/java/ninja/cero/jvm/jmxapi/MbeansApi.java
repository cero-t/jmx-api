package ninja.cero.jvm.jmxapi;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
@Configuration
@RestController
public class MbeansApi implements ApplicationListener<ContextClosedEvent> {
    public static void main(String[] args) {
        SpringApplication.run(MbeansApi.class, args);
    }

    @Bean
    public Jackson2ObjectMapperBuilder mapperBuilder() {
        return Jackson2ObjectMapperBuilder.json()
                .indentOutput(true);
    }

    JmxUtil jmxUtil = new JmxUtil();

    @RequestMapping("/")
    public List<JmxUtil.Jps> jps() {
        return jmxUtil.jps();
    }

    @RequestMapping("/{pid}/info")
    public JmxUtil.Jinfo jinfo(@PathVariable String pid) throws IOException, AttachNotSupportedException {
        return jmxUtil.jinfo(pid);
    }

    @RequestMapping("/{pid}")
    public Set<String> mbeans(@PathVariable String pid) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        return jmxUtil.mbeans(pid);
    }

    @RequestMapping("/{pid}/{name:.+}")
    public Map<String, Object> mbeansInfo(@PathVariable String pid, @PathVariable String name)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        return jmxUtil.mbeansInfo(pid, name);
    }

    @RequestMapping("/{pid}/{name:.+}/{keys}")
    public Object mbeansAttribute(@PathVariable String pid, @PathVariable String name, @PathVariable String keys[], @RequestParam Map<String, String> params)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException, MBeanException {
        return jmxUtil.mbeansAttributeOrInvoke(pid, name, keys, params);
    }

//    @RequestMapping("/{pid}/{name:.+}/operation/{operation}")
//    public Object mbeansInvoke(@PathVariable String pid, @PathVariable String name, @PathVariable String operation, @RequestParam Map<String, String> params)
//            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException, MBeanException {
//        return jmxUtil.mbeansInvoke(pid, name, operation, params);
//    }

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        jmxUtil.close();
    }
}
