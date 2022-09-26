package de.samply.share.broker.model;

import java.util.List;

/**
 * QueryContainer includes a generic query like cql or structured query and the target as a list.
 * @param <T> the generic query type
 */
public class QueryContainer<T> {

  private T query;
  private List<String> target;
  private String queryName;

  public T getQuery() {
    return query;
  }

  public void setQuery(T query) {
    this.query = query;
  }

  public String getQueryName() {
    return queryName;
  }

  public void setQueryName(String queryName) {
    this.queryName = queryName;
  }

  public List<String> getTarget() {
    return target;
  }

  public void setTarget(List<String> target) {
    this.target = target;
  }

}
