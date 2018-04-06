package ee.ut.basar;

import static java.lang.Math.sqrt;
import java.util.ArrayList;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.allocationpolicies.power.PowerVmAllocationPolicyAbstract;

public class PowerVmAllocationPolicyTest extends PowerVmAllocationPolicyAbstract {
    /**
     * Instantiates a new PowerVmAllocationPolicySimple.
     *
     */
    public PowerVmAllocationPolicyTest() {
        super();
    }

    /**
     * 
     * @param vmList the list of VMs
     * @return a map to perform optimization
     */
    @Override
    public Map<Vm, Host> optimizeAllocation(List<? extends Vm> vmList) {
        // Use Underload and Overload as metrics
        final Map<Vm, Host> migrationMap = new HashMap<>();
        List<Vm> migration = new ArrayList<>();
        List<Host> hostlist = getHostList();
        List<Host> available = new ArrayList<Host>(getHostList());
        for (Host host : hostlist) {
            int hostTotal = 0;
            if (!host.isActive()) {
                continue;
            }
            if (host.getAvailableMips() < 0.2 * host.getTotalMipsCapacity()) {
                migration.addAll(host.getVmList());
                List<Vm> vms = host.getVmList();
                for ( Vm vm : vms) {
                    hostTotal += vm.getNumberOfPes();
                    if (hostTotal > (host.getNumberOfPes() * 0.8)) {
                        break;
                    }
                    migration.remove(vm);
                }
                available.remove(host);
            }
        }
        for (Host host : available) {
            int hostTotal = 0;
            if (!host.isActive()) {
                continue;
            }
            List<Vm> vmsToMigrate = new ArrayList<>();
            if(migration.size() < 1) {
                boolean active = false;
                for (Vm vm : host.getVmList()) {
                    if (!vm.getCloudletScheduler().isEmpty()) {
                        active = true;
                    }
                }
                if (!active) {
                    host.setActive(false);
                    System.out.println("Host Power Off" + host.toString());
                }
            }
            for ( Vm vm : migration) {
                hostTotal += vm.getNumberOfPes();
                if (hostTotal > host.getNumberOfPes() * 0.8) {
                    break;
                }
                vmsToMigrate.add(vm);
            }
            for ( Vm vm : vmsToMigrate) {
                migrationMap.put(vm, host);
                migration.remove(vm);
            }
        }
        if (migration.size() > 0) {
            // Start other VM
            Host tmp = null;
            for (Host host : getHostList()) {
                if (!host.isActive()) {
                    host.setActive(true);
                    tmp = host;
                    break;
                }
            }
            for (Vm vm : migration) {
                if (tmp == null) {
                    break;
                }
                migrationMap.put(vm, tmp);
            }
        }
        return migrationMap;
    }
    
    private float fitness(Map<Vm, Host> sol) {
        float res = (float) 0.0;
        /*for t in range(len(sol)):
            for target in range(len(processes)):
                for source in range(len(processes)):
                    if sol[t][source][target] > 0:
                        res += sol[t][source][target] * processes[source]["price_eq"];*/
        return res;
    }
    
    private Map<Vm, Host> sanneal(List<Host> hosts, Map<Vm, Host> c_sol, float alpha, float t_criteria, float i_criteria ){
        Map<Vm, Host> cur_solution = c_sol;
        Map<Vm, Host> best_solution = cur_solution;
        int N = hosts.size();
        float current_fitness = (float) 0.0;
        current_fitness = fitness(cur_solution);
        float best_fitness = current_fitness;
        float temp = (float) sqrt(N);
        int itr = 1;
        while(temp >= t_criteria && itr < i_criteria) {
            Map<Vm, Host> can = cur_solution;

            float candidate_fitness = fitness(can);
            if(candidate_fitness < current_fitness) {
                cur_solution = can;
                current_fitness = candidate_fitness;

                if (candidate_fitness < best_fitness){
                    System.out.println("Fitness :: " + best_fitness);
                    best_fitness = candidate_fitness;
                    best_solution = can;
                }
            } else {
                if(Math.random() < Math.exp(-Math.abs(candidate_fitness-current_fitness) / temp) ){
                    cur_solution = can;
                    current_fitness = candidate_fitness;
                }

                temp *= alpha;
                itr += 1;
            }
        }
        return best_solution;
    }
}
    
