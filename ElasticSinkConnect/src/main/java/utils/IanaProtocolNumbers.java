package utils;

import java.util.HashMap;

/**
 * IanaProtocolNumbers class. Contains a set with mappings between network protocols
 * and theirs IANA numbers.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class IanaProtocolNumbers {
    /**
     * Map containing pairs (network protocol, IANA number).
     */
    public HashMap<String, Integer> protocolNum = new HashMap<String, Integer>();

    /**
     * Constructor. Initializes map with pairs.
     */
    public IanaProtocolNumbers() {
        // Initialize map with all IANA protocol numbers
        protocolNum.put("HOPOPT", 0);
        protocolNum.put("ICMP", 1);
        protocolNum.put("IGMP", 2);
        protocolNum.put("GGP", 3);
        protocolNum.put("IPv4", 4);
        protocolNum.put("ST", 5);
        protocolNum.put("TCP", 6);
        protocolNum.put("CBT", 7);
        protocolNum.put("EGP", 8);
        protocolNum.put("IGP", 9);
        protocolNum.put("BBN-RCC-MON", 10);
        protocolNum.put("NVP-II", 11);
        protocolNum.put("PUP", 12);
        protocolNum.put("ARGUS", 13);
        protocolNum.put("EMCON", 14);
        protocolNum.put("XNET", 15);
        protocolNum.put("CHAOS", 16);
        protocolNum.put("UDP", 17);
        protocolNum.put("MUX", 18);
        protocolNum.put("DCN-MEAS", 19);
        protocolNum.put("HMP", 20);
        protocolNum.put("PRM", 21);
        protocolNum.put("XNS-IDP", 22);
        protocolNum.put("TRUNK-1", 23);
        protocolNum.put("TRUNK-2", 24);
        protocolNum.put("LEAF-1", 25);
        protocolNum.put("LEAF-2", 26);
        protocolNum.put("RDP", 27);
        protocolNum.put("IRTP", 28);
        protocolNum.put("ISO-TP4", 29);
        protocolNum.put("NETBLT", 30);
        protocolNum.put("MFE-NSP", 31);
        protocolNum.put("MERIT-INP", 32);
        protocolNum.put("DCCP", 33);
        protocolNum.put("3PC", 34);
        protocolNum.put("IDPR", 35);
        protocolNum.put("XTP", 36);
        protocolNum.put("DDP", 37);
        protocolNum.put("IDPR-CMTP", 38);
        protocolNum.put("TP++", 39);
        protocolNum.put("IL", 40);
        protocolNum.put("IPv6", 41);
        protocolNum.put("SDRP", 42);
        protocolNum.put("IPv6-Route", 43);
        protocolNum.put("IPv6-Frag", 44);
        protocolNum.put("IDRP", 45);
        protocolNum.put("RSVP", 46);
        protocolNum.put("GRE", 47);
        protocolNum.put("DSR", 48);
        protocolNum.put("BNA", 49);
        protocolNum.put("ESP", 50);
        protocolNum.put("AH", 51);
        protocolNum.put("I-NLSP", 52);
        protocolNum.put("SWIPE", 53);
        protocolNum.put("NARP", 54);
        protocolNum.put("MOBILE", 55);
        protocolNum.put("TLSP", 56);
        protocolNum.put("SKIP", 57);
        protocolNum.put("IPv6-ICMP", 58);
        protocolNum.put("IPv6-NoNxt", 59);
        protocolNum.put("IPv6-Opts", 60);
        protocolNum.put("any", 61);
        protocolNum.put("CFTP", 62);
        protocolNum.put("any", 63);
        protocolNum.put("SAT-EXPAK", 64);
        protocolNum.put("KRYPTOLAN", 65);
        protocolNum.put("RVD", 66);
        protocolNum.put("IPPC", 67);
        protocolNum.put("any", 68);
        protocolNum.put("SAT-MON", 69);
        protocolNum.put("VISA", 70);
        protocolNum.put("IPCV", 71);
        protocolNum.put("CPNX", 72);
        protocolNum.put("CPHB", 73);
        protocolNum.put("WSN", 74);
        protocolNum.put("PVP", 75);
        protocolNum.put("BR-SAT-MON", 76);
        protocolNum.put("SUN-ND", 77);
        protocolNum.put("WB-MON", 78);
        protocolNum.put("WB-EXPAK", 79);
        protocolNum.put("ISO-IP", 80);
        protocolNum.put("VMTP", 81);
        protocolNum.put("SECURE-VMTP", 82);
        protocolNum.put("VINES", 83);
        protocolNum.put("TTP", 84);
        protocolNum.put("IPTM", 84);
        protocolNum.put("NSFNET-IGP", 85);
        protocolNum.put("DGP", 86);
        protocolNum.put("TCF", 87);
        protocolNum.put("EIGRP", 88);
        protocolNum.put("OSPFIGP", 89);
        protocolNum.put("Sprite-RPC", 90);
        protocolNum.put("LARP", 91);
        protocolNum.put("MTP", 92);
        protocolNum.put("AX.25", 93);
        protocolNum.put("IPIP", 94);
        protocolNum.put("MICP", 95);
        protocolNum.put("SCC-SP", 96);
        protocolNum.put("ETHERIP", 97);
        protocolNum.put("ENCAP", 98);
        protocolNum.put("any", 99);
        protocolNum.put("GMTP", 100);
        protocolNum.put("IFMP", 101);
        protocolNum.put("PNNI", 102);
        protocolNum.put("PIM", 103);
        protocolNum.put("ARIS", 104);
        protocolNum.put("SCPS", 105);
        protocolNum.put("QNX", 106);
        protocolNum.put("A/N", 107);
        protocolNum.put("IPComp", 108);
        protocolNum.put("SNP", 109);
        protocolNum.put("Compaq-Peer", 110);
        protocolNum.put("IPX-in-IP", 111);
        protocolNum.put("VRRP", 112);
        protocolNum.put("PGM", 113);
        protocolNum.put("any", 114);
        protocolNum.put("L2TP", 115);
        protocolNum.put("DDX", 116);
        protocolNum.put("IATP", 117);
        protocolNum.put("STP", 118);
        protocolNum.put("SRP", 119);
        protocolNum.put("UTI", 120);
        protocolNum.put("SMP", 121);
        protocolNum.put("SM", 122);
        protocolNum.put("PTP", 123);
        protocolNum.put("ISIS", 124);
        protocolNum.put("FIRE", 125);
        protocolNum.put("CRTP", 126);
        protocolNum.put("CRUDP", 127);
        protocolNum.put("SSCOPMCE", 128);
        protocolNum.put("IPLT", 129);
        protocolNum.put("SPS", 130);
        protocolNum.put("PIPE", 131);
        protocolNum.put("SCTP", 132);
        protocolNum.put("FC", 133);
        protocolNum.put("RSVP-E2E-IGNORE", 134);
        protocolNum.put("Mobility", 135);
        protocolNum.put("UDPLite", 136);
        protocolNum.put("MPLS-in-IP", 137);
        protocolNum.put("manet", 138);
        protocolNum.put("HIP", 139);
        protocolNum.put("Shim6", 140);
        protocolNum.put("WESP", 141);
        protocolNum.put("ROHC", 142);
        protocolNum.put("Ethernet", 143);
        protocolNum.put("Unassigned", 144);
        protocolNum.put("Unassigned", 145);
        protocolNum.put("Unassigned", 146);
        protocolNum.put("Unassigned", 147);
        protocolNum.put("Unassigned", 148);
        protocolNum.put("Unassigned", 149);
        protocolNum.put("Unassigned", 150);
        protocolNum.put("Unassigned", 151);
        protocolNum.put("Unassigned", 152);
        protocolNum.put("Unassigned", 153);
        protocolNum.put("Unassigned", 154);
        protocolNum.put("Unassigned", 155);
        protocolNum.put("Unassigned", 156);
        protocolNum.put("Unassigned", 157);
        protocolNum.put("Unassigned", 158);
        protocolNum.put("Unassigned", 159);
        protocolNum.put("Unassigned", 160);
        protocolNum.put("Unassigned", 161);
        protocolNum.put("Unassigned", 162);
        protocolNum.put("Unassigned", 163);
        protocolNum.put("Unassigned", 164);
        protocolNum.put("Unassigned", 165);
        protocolNum.put("Unassigned", 166);
        protocolNum.put("Unassigned", 167);
        protocolNum.put("Unassigned", 168);
        protocolNum.put("Unassigned", 169);
        protocolNum.put("Unassigned", 170);
        protocolNum.put("Unassigned", 171);
        protocolNum.put("Unassigned", 172);
        protocolNum.put("Unassigned", 173);
        protocolNum.put("Unassigned", 174);
        protocolNum.put("Unassigned", 175);
        protocolNum.put("Unassigned", 176);
        protocolNum.put("Unassigned", 177);
        protocolNum.put("Unassigned", 178);
        protocolNum.put("Unassigned", 179);
        protocolNum.put("Unassigned", 180);
        protocolNum.put("Unassigned", 181);
        protocolNum.put("Unassigned", 182);
        protocolNum.put("Unassigned", 183);
        protocolNum.put("Unassigned", 184);
        protocolNum.put("Unassigned", 185);
        protocolNum.put("Unassigned", 186);
        protocolNum.put("Unassigned", 187);
        protocolNum.put("Unassigned", 188);
        protocolNum.put("Unassigned", 189);
        protocolNum.put("Unassigned", 190);
        protocolNum.put("Unassigned", 191);
        protocolNum.put("Unassigned", 192);
        protocolNum.put("Unassigned", 193);
        protocolNum.put("Unassigned", 194);
        protocolNum.put("Unassigned", 195);
        protocolNum.put("Unassigned", 196);
        protocolNum.put("Unassigned", 197);
        protocolNum.put("Unassigned", 198);
        protocolNum.put("Unassigned", 199);
        protocolNum.put("Unassigned", 200);
        protocolNum.put("Unassigned", 201);
        protocolNum.put("Unassigned", 202);
        protocolNum.put("Unassigned", 203);
        protocolNum.put("Unassigned", 204);
        protocolNum.put("Unassigned", 205);
        protocolNum.put("Unassigned", 206);
        protocolNum.put("Unassigned", 207);
        protocolNum.put("Unassigned", 208);
        protocolNum.put("Unassigned", 209);
        protocolNum.put("Unassigned", 210);
        protocolNum.put("Unassigned", 211);
        protocolNum.put("Unassigned", 212);
        protocolNum.put("Unassigned", 213);
        protocolNum.put("Unassigned", 214);
        protocolNum.put("Unassigned", 215);
        protocolNum.put("Unassigned", 216);
        protocolNum.put("Unassigned", 217);
        protocolNum.put("Unassigned", 218);
        protocolNum.put("Unassigned", 219);
        protocolNum.put("Unassigned", 220);
        protocolNum.put("Unassigned", 221);
        protocolNum.put("Unassigned", 222);
        protocolNum.put("Unassigned", 223);
        protocolNum.put("Unassigned", 224);
        protocolNum.put("Unassigned", 225);
        protocolNum.put("Unassigned", 226);
        protocolNum.put("Unassigned", 227);
        protocolNum.put("Unassigned", 228);
        protocolNum.put("Unassigned", 229);
        protocolNum.put("Unassigned", 230);
        protocolNum.put("Unassigned", 231);
        protocolNum.put("Unassigned", 232);
        protocolNum.put("Unassigned", 233);
        protocolNum.put("Unassigned", 234);
        protocolNum.put("Unassigned", 235);
        protocolNum.put("Unassigned", 236);
        protocolNum.put("Unassigned", 237);
        protocolNum.put("Unassigned", 238);
        protocolNum.put("Unassigned", 239);
        protocolNum.put("Unassigned", 240);
        protocolNum.put("Unassigned", 241);
        protocolNum.put("Unassigned", 242);
        protocolNum.put("Unassigned", 243);
        protocolNum.put("Unassigned", 244);
        protocolNum.put("Unassigned", 245);
        protocolNum.put("Unassigned", 246);
        protocolNum.put("Unassigned", 247);
        protocolNum.put("Unassigned", 248);
        protocolNum.put("Unassigned", 249);
        protocolNum.put("Unassigned", 250);
        protocolNum.put("Unassigned", 251);
        protocolNum.put("Unassigned", 252);
        protocolNum.put("Use", 253);
        protocolNum.put("Use", 254);
        protocolNum.put("Reserved", 255);
    }

    /**
     * Get IANA number, given a protocol's name.
     *
     * @param protocol String Network protocol name (i.e. TCP, UDP, etc).
     * @return Integer IANA number.
     */
    public int getIanaNum(String protocol) {

        if(protocolNum.containsKey(protocol)) {
            return protocolNum.get(protocol);
        }
        else {
            return protocolNum.get("TCP");
        }
    }
}
