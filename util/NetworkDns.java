
import android.net.Network;
import android.os.Build;

import org.jetbrains.annotations.NotNull;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;

public class NetworkDns implements Dns {

    private static NetworkDns sInstance;
    private Network mNetwork;

    public static NetworkDns getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkDns();
        }
        return sInstance;
    }

    public void setNetwork(Network network) {
        mNetwork = network;
    }

    @NotNull
    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        // #24 - improve ways to resolve hostname if network cannot resolve
        // case: wifi + 4G
        if (mNetwork != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                List<InetAddress> inetAddressList = new ArrayList<>();
                InetAddress[] inetAddresses = mNetwork.getAllByName(hostname);
                for (InetAddress inetAddress : inetAddresses) {
                    // priority ipv6 first
                    if (!(inetAddress instanceof Inet4Address)) {
                        inetAddressList.add(0, inetAddress);
                    } else {
                        inetAddressList.add(inetAddress);
                    }
                }
                return inetAddressList;
            } catch (NullPointerException | UnknownHostException ex) {
                try {
                    return Dns.SYSTEM.lookup(hostname);
                } catch (UnknownHostException e) {
                    return Arrays.asList(InetAddress.getAllByName(hostname));
                }
            }
        }

        // case mobile only
        try {
            List<InetAddress> inetAddressList = new ArrayList<>();
            InetAddress[] inetAddresses = InetAddress.getAllByName(hostname);
            for (InetAddress inetAddress : inetAddresses) {
                // priority ipv6 first
                if (!(inetAddress instanceof Inet4Address)) {
                    inetAddressList.add(0, inetAddress);
                } else {
                    inetAddressList.add(inetAddress);
                }
            }
            return inetAddressList;
        } catch (NullPointerException | UnknownHostException ex) {
            return Dns.SYSTEM.lookup(hostname);
        }
    }

    private NetworkDns() {
    }
}
