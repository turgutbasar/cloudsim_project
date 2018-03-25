package ee.ut.basar;

import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.power.PowerHostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;

public class TestForSeminar {

    private static final int HOST_PES_NUMBER = 20;

    private static final int VM_PES_NUMBER = 3;

    private static final int NUMBER_OF_CLOUDLETS = 100;

    private final List<Host> hostList;
    private final List<Vm> vmList;
    private final List<Cloudlet> cloudletList;
    private final DatacenterBroker broker;
    private final Datacenter datacenter;
    private final CloudSim simulation;

    public static void main(String[] args) {
        Log.printFormattedLine("Starting %s ...", TestForSeminar.class.getSimpleName());
        new TestForSeminar();
        Log.printFormattedLine("%s finished!", TestForSeminar.class.getSimpleName());
    }

    public TestForSeminar() {
        simulation = new CloudSim();

        this.hostList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
        this.datacenter = createDatacenter();
        this.broker = new DatacenterBrokerSimple(simulation);

        createAndSubmitVms();
        for (Vm vm : vmList) {
            createAndSetCloudlets(vm);
        }
        this.broker.submitCloudletList(cloudletList);
        
        runSimulationAndPrintResults();
    }

    private void runSimulationAndPrintResults() {
        simulation.start();

        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();
    }

    private void createAndSetCloudlets(Vm vm) {
        int cloudletId;
        long length = 10000;
        int num = vm.getId() != 0 ? 1 : (int)(Math.random()*NUMBER_OF_CLOUDLETS);
        for(int i = 0; i < num; i++){
            cloudletId = vm.getId() * 1000 + i;
            Cloudlet cloudlet = createCloudlet(cloudletId, vm, (int)(Math.random()*length));
            this.cloudletList.add(cloudlet);
        }
    }

    private void createAndSubmitVms() {
        for (int i = 0; i < 5; i++) {
            this.vmList.add(createVm(i));
        }
        this.broker.submitVmList(vmList);
    }

    private Vm createVm(int id) {
        int mips = 1000;
        long size = 10000; // image size (MEGABYTE)
        int ram = 512; // vm memory (MEGABYTE)
        long bw = 1000;
        Vm vm = new VmSimple(id, mips, VM_PES_NUMBER)
            .setRam(ram).setBw(bw).setSize(size)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared());

        return vm;
    }

    private Cloudlet createCloudlet(int id, Vm vm, long length) {
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = (int)(Math.random()*4) + 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        Cloudlet cloudlet
            = new CloudletSimple(id, (int)(Math.random()*length) + 1, pesNumber)
                    .setFileSize(fileSize)
                    .setOutputSize(outputSize)
                    .setUtilizationModel(utilizationModel)
                    .setVm(vm);
        
        return cloudlet;
    }

    private Datacenter createDatacenter() {
        for (int i = 0; i < 5; i++) {
            this.hostList.add(createHost(i));
        }
        DatacenterCharacteristics characteristics = new DatacenterCharacteristicsSimple(hostList);
        return new DatacenterPowerSaverExample(simulation, characteristics, new PowerVmAllocationPolicyTest());
    }

    private Host createHost(int id) {
        List<Pe> peList = new ArrayList<>();
        long mips = 1000;
        for(int i = 0; i < HOST_PES_NUMBER; i++){
            peList.add(new PeSimple(mips, new PeProvisionerSimple()));
        }
        long ram = 2048; // host memory (MEGABYTEs)
        long storage = 1000000; // host storage (MEGABYTEs)
        long bw = 10000; //Megabits/s

        return new PowerHostSimple(ram, bw, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerTimeShared());
    }
}
