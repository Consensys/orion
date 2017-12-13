package net.consensys.athena.impl.network;

import net.consensys.athena.api.network.PartyInfo;

import java.net.URL;
import java.util.HashSet;

public class MemoryPartyInfor implements PartyInfo {

  private URL url;
  private HashSet<URL> parties;

  public void setUrl(URL url) {
    this.url = url;
  }

  public void setParties(HashSet<URL> parties) {
    this.parties = parties;
  }

  public void addNodeURL(URL node) {
    this.parties.add(node);
  }

  @Override
  public URL url() {
    return url;
  }

  @Override
  public HashSet<URL> parties() {
    return parties;
  }
}
