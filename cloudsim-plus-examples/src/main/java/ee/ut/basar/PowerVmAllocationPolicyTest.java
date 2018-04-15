package ee.ut.basar;

import static java.lang.Math.sqrt;
import java.util.ArrayList;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.cloudbus.cloudsim.allocationpolicies.power.PowerVmAllocationPolicyAbstract;

public class PowerVmAllocationPolicyTest extends PowerVmAllocationPolicyAbstract {
    
    private final Boolean useSimulatedAnnealing;
    
    private final double alpha;
    
    private final double t_criteria;
    
    private final double i_criteria;
    
    /**
     * Instantiates a new PowerVmAllocationPolicySimple.
     *
     * @param useSimulatedAnnealing
     * @param alpha
     * @param t_criteria
     * @param i_criteria
     */
    public PowerVmAllocationPolicyTest(Boolean useSimulatedAnnealing, double alpha, double t_criteria, double i_criteria) {
        super();
        this.useSimulatedAnnealing = useSimulatedAnnealing;
        this.alpha = alpha;
        this.t_criteria = t_criteria;
        this.i_criteria = i_criteria;
    }

    /**
     * 
     * @param vmList the list of VMs
     * @return a map to perform optimization
     */
    @Override
    public Map<Vm, Host> optimizeAllocation(List<? extends Vm> vmList) {
        
        // Run vanilla implementation first
        final Map<Vm, Host> migrationMap = vanilla(vmList);
        List<Host> hostlist = getHostList();
        // According to config, if needed run simulated annealing with result of vanilla to improve
        if (!useSimulatedAnnealing)
            return migrationMap;
        else
            return sanneal(hostlist, (List<Vm>)vmList, migrationMap, alpha, t_criteria, i_criteria);
    }
    
    private Map<Vm, Host> shuffle(Map<Vm, Host> sol, List<Vm> vms, List<Host> hosts) {
        Map<Vm, Host> shuffeled = new HashMap<>();
        int split = new Random().nextInt(vms.size());
        split = split == 0 ? 0 : split-1;
        List<Vm> keeping = vms.subList(0, split);
        split = split == 0 ? 0 : split+1;
        List<Vm> random = vms.subList(split, vms.size());
        
        for (Vm vm : keeping) {
            if (sol.get(vm) != null)
                shuffeled.put(vm, sol.get(vm));
        }
        
        for (Vm vm : random) {
            int rand_host = new Random().nextInt(hosts.size()+1);
            if (rand_host <= hosts.size())
                shuffeled.put(vm, hosts.get(new Random().nextInt(hosts.size())));
        }
        return shuffeled;
    }
    
    private double fitness(List<Host> hosts, Map<Vm, Host> sol) {
        double cost = 0.0;
        Map<Host, Vm> hostList_clone = new HashMap<>();
        for (Host host : hosts) {
            List<Vm> host_vms = host.getVmList();
            for (Vm host_vm : host_vms) {
                hostList_clone.put(host, host_vm);
            }
        }
        
        for (Map.Entry<Vm, Host> entry : sol.entrySet()) {
            Vm key = entry.getKey();
            Host value = entry.getValue();
            hostList_clone.remove(value);
            hostList_clone.put(value, key);
            cost += 100; // Static cost per migration
        }
        
        for (Host host : hostList_clone.keySet()) {
            cost += 1000; // Static cost per machine same period
        }
        return cost;
    }
    
    /*Simulated Annealing Algorithm*/
    private Map<Vm, Host> sanneal(List<Host> hosts, List<Vm> vms, Map<Vm, Host> initial_sol, double alpha, double t_criteria, double i_criteria){
        Map<Vm, Host> cur_solution = initial_sol;
        Map<Vm, Host> best_solution = cur_solution;
        int N = hosts.size();
        double current_fitness = fitness(hosts, cur_solution);
        double best_fitness = current_fitness;
        float temp = (float) sqrt(N);
        int itr = 1;
        while(temp >= t_criteria && itr < i_criteria) {
            Map<Vm, Host> can = shuffle(cur_solution, vms, hosts);
            double candidate_fitness = fitness(hosts, can);
            if(candidate_fitness < current_fitness) {
                cur_solution = can;
                current_fitness = candidate_fitness;
                if (candidate_fitness < best_fitness){
                    System.out.println("Improvement :: " + best_fitness);
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

    private Map<Vm, Host> vanilla(List<? extends Vm> vmList) {
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
}
    
