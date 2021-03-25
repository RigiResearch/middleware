package com.rigiresearch.middleware.vmware.hcl.agent;

import lombok.Getter;

/**
 * Names for VMware VM operating systems based on the vSphere API.
 * Ids taken from https://www.vmware.com/support/developer/converter-sdk/conv60_apireference/vim.vm.GuestOsDescriptor.GuestOsIdentifier.html
 * and constants taken from vSphere's API.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum VMwareOS {
    ASIANUX_3("asianux3Guest"),
    ASIANUX_3_64("asianux3_64Guest"),
    ASIANUX_4("asianux4Guest"),
    ASIANUX_4_64("asianux4_64Guest"),
    ASIANUX_5_64("asianux5_64Guest"),
    ASIANUX_7_64("asianux7_64Guest"),
    CENTOS("centosGuest"),
    CENTOS_6("centos6Guest"),
    CENTOS_64("centos64Guest"),
    CENTOS_6_64("centos6_64Guest"),
    CENTOS_7("centos7Guest"),
    CENTOS_7_64("centos7_64Guest"),
    COREOS_64("coreos64Guest"),
    DARWIN("darwinGuest"),
    DARWIN_10("darwin10Guest"),
    DARWIN_10_64("darwin10_64Guest"),
    DARWIN_11("darwin11Guest"),
    DARWIN_11_64("darwin11_64Guest"),
    DARWIN_12_64("darwin12_64Guest"),
    DARWIN_13_64("darwin13_64Guest"),
    DARWIN_14_64("darwin14_64Guest"),
    DARWIN_15_64("darwin15_64Guest"),
    DARWIN_16_64("darwin16_64Guest"),
    DARWIN_64("darwin64Guest"),
    DEBIAN_10("debian10Guest"),
    DEBIAN_10_64("debian10_64Guest"),
    DEBIAN_4("debian4Guest"),
    DEBIAN_4_64("debian4_64Guest"),
    DEBIAN_5("debian5Guest"),
    DEBIAN_5_64("debian5_64Guest"),
    DEBIAN_6("debian6Guest"),
    DEBIAN_6_64("debian6_64Guest"),
    DEBIAN_7("debian7Guest"),
    DEBIAN_7_64("debian7_64Guest"),
    DEBIAN_8("debian8Guest"),
    DEBIAN_8_64("debian8_64Guest"),
    DEBIAN_9("debian9Guest"),
    DEBIAN_9_64("debian9_64Guest"),
    DOS("dosGuest"),
    ECOMSTATION("eComStationGuest"),
    ECOMSTATION_2("eComStation2Guest"),
    FEDORA("fedoraGuest"),
    FEDORA_64("fedora64Guest"),
    FREEBSD("freebsdGuest"),
    FREEBSD_64("freebsd64Guest"),
    GENERIC_LINUX("genericLinuxGuest"),
    MANDRAKE("mandrakeGuest"),
    MANDRIVA("mandrivaGuest"),
    MANDRIVA_64("mandriva64Guest"),
    NETWARE_4("netware4Guest"),
    NETWARE_5("netware5Guest"),
    NETWARE_6("netware6Guest"),
    NLD_9("nld9Guest"),
    OES("oesGuest"),
    OPENSERVER_5("openServer5Guest"),
    OPENSERVER_6("openServer6Guest"),
    OPENSUSE("opensuseGuest"),
    OPENSUSE_64("opensuse64Guest"),
    ORACLE_LINUX("oracleLinuxGuest"),
    ORACLE_LINUX_6("oracleLinux6Guest"),
    ORACLE_LINUX_64("oracleLinux64Guest"),
    ORACLE_LINUX_6_64("oracleLinux6_64Guest"),
    ORACLE_LINUX_7("oracleLinux7Guest"),
    ORACLE_LINUX_7_64("oracleLinux7_64Guest"),
    OS2("os2Guest"),
    OTHER("otherGuest"),
    OTHER_24X_LINUX("other24xLinuxGuest"),
    OTHER_24X_LINUX_64("other24xLinux64Guest"),
    OTHER_26X_LINUX("other26xLinuxGuest"),
    OTHER_26X_LINUX_64("other26xLinux64Guest"),
    OTHER_3X_LINUX("other3xLinuxGuest"),
    OTHER_3X_LINUX_64("other3xLinux64Guest"),
    OTHER_64("otherGuest64"),
    OTHER_LINUX("otherLinuxGuest"),
    OTHER_LINUX_64("otherLinux64Guest"),
    REDHAT("redhatGuest"),
    RHEL_2("rhel2Guest"),
    RHEL_3("rhel3Guest"),
    RHEL_3_64("rhel3_64Guest"),
    RHEL_4("rhel4Guest"),
    RHEL_4_64("rhel4_64Guest"),
    RHEL_5("rhel5Guest"),
    RHEL_5_64("rhel5_64Guest"),
    RHEL_6("rhel6Guest"),
    RHEL_6_64("rhel6_64Guest"),
    RHEL_7("rhel7Guest"),
    RHEL_7_64("rhel7_64Guest"),
    SJDS("sjdsGuest"),
    SLES("slesGuest"),
    SLES_10("sles10Guest"),
    SLES_10_64("sles10_64Guest"),
    SLES_11("sles11Guest"),
    SLES_11_64("sles11_64Guest"),
    SLES_12("sles12Guest"),
    SLES_12_64("sles12_64Guest"),
    SLES_64("sles64Guest"),
    SOLARIS_10("solaris10Guest"),
    SOLARIS_10_64("solaris10_64Guest"),
    SOLARIS_11_64("solaris11_64Guest"),
    SOLARIS_6("solaris6Guest"),
    SOLARIS_7("solaris7Guest"),
    SOLARIS_8("solaris8Guest"),
    SOLARIS_9("solaris9Guest"),
    SUSE("suseGuest"),
    SUSE_64("suse64Guest"),
    TURBO_LINUX("turboLinuxGuest"),
    TURBO_LINUX_64("turboLinux64Guest"),
    UBUNTU("ubuntuGuest"),
    UBUNTU_64("ubuntu64Guest"),
    UNIXWARE_7("unixWare7Guest"),
    VMKERNEL("vmkernelGuest"),
    VMKERNEL_5("vmkernel5Guest"),
    VMKERNEL_6("vmkernel6Guest"),
    VMKERNEL_65("vmkernel65Guest"),
    VMWARE_PHOTON_64(""),
    WINDOWS_7("windows7Guest"),
    WINDOWS_7_64("windows7_64Guest"),
    WINDOWS_7_SERVER_64("windows7Server64Guest"),
    WINDOWS_8("windows8Guest"),
    WINDOWS_8_64("windows8_64Guest"),
    WINDOWS_8_SERVER_64("windows8Server64Guest"),
    WINDOWS_9("windows9Guest"),
    WINDOWS_9_64("windows9_64Guest"),
    WINDOWS_9_SERVER_64("windows9Server64Guest"),
    WINDOWS_HYPERV("windowsHyperVGuest"),
    WIN_2000_ADV_SERV("win2000AdvServGuest"),
    WIN_2000_PRO("win2000ProGuest"),
    WIN_2000_SERV("win2000ServGuest"),
    WIN_31("win31Guest"),
    WIN_95("win95Guest"),
    WIN_98("win98Guest"),
    WIN_LONGHORN("winLonghornGuest"),
    WIN_LONGHORN_64("winLonghorn64Guest"),
    WIN_ME("winMeGuest"),
    WIN_NET_BUSINESS("winNetBusinessGuest"),
    WIN_NET_DATACENTER("winNetDatacenterGuest"),
    WIN_NET_DATACENTER_64("winNetDatacenter64Guest"),
    WIN_NET_ENTERPRISE("winNetEnterpriseGuest"),
    WIN_NET_ENTERPRISE_64("winNetEnterprise64Guest"),
    WIN_NET_STANDARD("winNetStandardGuest"),
    WIN_NET_STANDARD_64("winNetStandard64Guest"),
    WIN_NET_WEB("winNetWebGuest"),
    WIN_NT("winNTGuest"),
    WIN_VISTA("winVistaGuest"),
    WIN_VISTA_64("winVista64Guest"),
    WIN_XP_HOME("winXPHomeGuest"),
    WIN_XP_PRO("winXPProGuest"),
    WIN_XP_PRO_64("winXPPro64Guest");

    /**
     * The operating system id.
     */
    @Getter
    private final String id;

    /**
     * Default constructor.
     * @param id The operating system id
     */
    VMwareOS(final String id) {
        this.id = id;
    }

}
