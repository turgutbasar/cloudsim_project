package ee.ut.basar;

import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.Collections;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.power.PowerVmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.hosts.power.PowerHost;
import org.cloudbus.cloudsim.util.Log;

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
        List<Host> hostlist = getHostList()
                .stream()
                .sorted()
                .filter(h -> h.isActive())
                .collect(Collectors.toList());
        // According to config, if needed run simulated annealing with result of vanilla to improve
        if (!useSimulatedAnnealing)
            return migrationMap;
        else
            return sanneal(hostlist, (List<Vm>)vmList, migrationMap, alpha, t_criteria, i_criteria);
        
        
        // TODO : No Optimization ve Random Start Simulated Annealing Implement Edelim
    }
    
    public PowerHost findDeactiveHostForVm(Vm vm) {
        return this.<PowerHost>getHostList()
                .stream()
                .sorted()
                .filter(h -> !h.isActive())
                .filter(h -> h.isSuitableForVm(vm))
                .findFirst().orElse(PowerHost.NULL);
    }

    @Override
    public PowerHost findHostForVm(Vm vm) {
        return this.<PowerHost>getHostList()
                .stream()
                .sorted()
                .filter(h -> h.isActive())
                .filter(h -> h.isSuitableForVm(vm))
                .findFirst().orElse(PowerHost.NULL);
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        Host host = findHostForVm(vm);
        if (host == PowerHost.NULL) {
            Log.printFormattedLine("%.2f: No suitable active host found for VM #" + vm.getId() + "\n", vm.getSimulation().clock());
            host = findDeactiveHostForVm(vm);
            if (host == PowerHost.NULL) {
                Log.printFormattedLine("%.2f: No suitable host found for VM #" + vm.getId() + " at all.\n", vm.getSimulation().clock());
                return false;
            }
            host.setActive(true);
            Log.printFormattedLine("%.2f: Host #" + host.getId() + " is switched on.\n", vm.getSimulation().clock());
        }
        return allocateHostForVm(vm, host);
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        if (vm.getHost().getVmList().size() == 1) {
            // If after this vm host empty, we set it deactive
            vm.getHost().setActive(false);
            Log.printFormattedLine("%.2f: Host #" + vm.getHost().getId() + " is switched off.\n", vm.getSimulation().clock());
        }
        super.deallocateHostForVm(vm); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    private Map<Vm, Host> shuffle(Map<Vm, Host> sol, List<Vm> vms, List<Host> hosts) {
        // TODO : Keep track of temporary host statuses to prevent failed migrations.
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
        Map<Vm, Host> hostmap_clone = new HashMap<>();
        for (Host host : hosts) {
            List<Vm> host_vms = host.getVmList();
            for (Vm vm : host_vms) {
                hostmap_clone.put(vm, host);
            }
        }
        
        for (Map.Entry<Vm, Host> entry : sol.entrySet()) {
            Vm vm = entry.getKey();
            Host host = entry.getValue();
            hostmap_clone.remove(vm);
            hostmap_clone.put(vm, host);
            cost += 100; // Static cost per migration
        }
        
        List<Host> hlist = new ArrayList();
        for (Entry<Vm, Host> ent : hostmap_clone.entrySet()) {
            if (!hlist.contains(ent.getValue())) {
                hlist.add(ent.getValue());
                cost += 1000; // Static cost per machine same period
            }
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
                available.remove(host);
                continue;
            }
            // Overload Case
            double av = host.getAvailableMips();
            double total = host.getTotalMipsCapacity();
            if (host.getAvailableMips() < 0.2 * host.getTotalMipsCapacity()) {
                migration.addAll(host.getVmList());
                List<Vm> vms = host.getVmList();
                for (Vm vm : vms) {
                    if (vm.isInMigration()) {
                        migration.remove(vm);
                    }
                    hostTotal += vm.getMips();
                    if (hostTotal > (host.getTotalMipsCapacity() * 0.8)) {
                        break;
                    }
                    migration.remove(vm);
                }
                available.remove(host);
            }
            // TODO : Implement Underload
        }
        for (Host host : available) {
            List<Vm> vmsToMigrate = new ArrayList<>();
            int migDiffTotal = 0;/*
            for ( Vm vm : host.getVmsMigratingIn()){
                migDiffTotal += vm.getMips();
            }*/
            for ( Vm vm : migration) {
                if (host.getAvailableMips() - migDiffTotal - vm.getMips() < 0.2 * host.getTotalMipsCapacity()) {
                    break;
                }
                if (host.isSuitableForVm(vm)) {
                    vmsToMigrate.add(vm);
                    migDiffTotal += vm.getNumberOfPes();
                }
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
                    Log.printFormattedLine("%.2f: Host #" + host.getId() + " is switched on.\n", host.getSimulation().clock());
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
    
