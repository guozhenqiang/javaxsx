package com.eroelf.javaxsx.util.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.eroelf.javaxsx.util.StdLoggers;
import com.google.common.base.CaseFormat;

/**
 * Provides APIs for querying and updating databases.
 * 
 * @author weikun.zhong
 */
public class DoDb implements AutoCloseable
{
	private Connection connection=null;
	private PreparedStatement preparedStatement=null;
	private Statement statement=null;
	private ResultSet resultSet=null;

	public DoDb()
	{}

	public DoDb(Connection connection)
	{
		setConnection(connection);
	}

	public ResultSet executeQuery(Connection connection, boolean ifOnceForAllData, String querySql, Object... objects) throws SQLException
	{
		setConnection(connection);
		return executeQuery(ifOnceForAllData, querySql, objects);
	}

	public ResultSet executeQuery(boolean ifOnceForAllData, String querySql, Object... objects) throws SQLException
	{
		closePreparedStatement();
		prepareStatement(ifOnceForAllData, querySql);
		for(int i=0; i<objects.length; i++)
		{
			preparedStatement.setObject(i+1, objects[i]);
		}
		resultSet=preparedStatement.executeQuery();
		return resultSet;
	}

	public int executeUpdate(Connection connection, boolean ifClose, String updateSql, Object... objects) throws SQLException
	{
		setConnection(connection);
		return executeUpdate(ifClose, updateSql, objects);
	}

	public int executeUpdate(boolean ifClose, String updateSql, Object... objects) throws SQLException
	{
		int re=0;
		try
		{
			closePreparedStatement();
			prepareStatement(true, updateSql);
			for(int i=0; i<objects.length; i++)
			{
				preparedStatement.setObject(i+1, objects[i]);
			}
			re=preparedStatement.executeUpdate();
		}
		finally
		{
			if(ifClose)
				close();
		}
		return re;
	}

	public void prepareStatement(boolean ifOnceForAllData, String sql) throws SQLException
	{
		closePreparedStatement();
		if(ifOnceForAllData)
			preparedStatement=connection.prepareStatement(sql);
		else
		{
			preparedStatement=connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			preparedStatement.setFetchSize(Integer.MIN_VALUE);
		}
	}

	public ResultSet executeQueryAgain(Object... objects) throws SQLException
	{
		for(int i=0; i<objects.length; i++)
		{
			preparedStatement.setObject(i+1, objects[i]);
		}
		resultSet=preparedStatement.executeQuery();
		return resultSet;
	}

	public int executeUpdateAgain(boolean ifClose, Object... objects) throws SQLException
	{
		int re=0;
		try
		{
			for(int i=0; i<objects.length; i++)
			{
				preparedStatement.setObject(i+1, objects[i]);
			}
			re=preparedStatement.executeUpdate();
		}
		finally
		{
			if(ifClose)
				close();
		}
		return re;
	}

	public ResultSet executeStatementQuery(Connection connection, boolean ifOnceForAllData, String querySql) throws SQLException
	{
		setConnection(connection);
		return executeStatementQuery(ifOnceForAllData, querySql);
	}

	public ResultSet executeStatementQuery(boolean ifOnceForAllData, String querySql) throws SQLException
	{
		closeStatement();
		if(ifOnceForAllData)
			statement=connection.createStatement();
		else
		{
			statement=connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			statement.setFetchSize(Integer.MIN_VALUE);
		}
		resultSet=statement.executeQuery(querySql);
		return resultSet;
	}

	public int executeStatementUpdate(Connection connection, boolean ifClose, String updateSql) throws SQLException
	{
		setConnection(connection);
		return executeStatementUpdate(ifClose, updateSql);
	}

	public int executeStatementUpdate(boolean ifClose, String updateSql) throws SQLException
	{
		int re=0;
		try
		{
			closeStatement();
			statement=connection.createStatement();
			re=statement.executeUpdate(updateSql);
		}
		finally
		{
			if(ifClose)
				close();
		}
		return re;
	}

	public void addBatch(Object... objects) throws SQLException
	{
		for(int i=0; i<objects.length; i++)
		{
			preparedStatement.setObject(i+1, objects[i]);
		}
		preparedStatement.addBatch();
	}

	public int[] executeBatch() throws SQLException
	{
		return preparedStatement.executeBatch();
	}

	public void commit(boolean ifClose) throws SQLException
	{
		try
		{
			connection.commit();
		}
		finally
		{
			if(ifClose)
				close();
		}
	}

	public String preparedStatement2String()
	{
		return preparedStatement==null ? "" : preparedStatement.toString();
	}

	public String statement2String()
	{
		return statement==null ? "" : statement.toString();
	}

	public void setConnection(Connection connection)
	{
		this.connection=connection;
	}

	private void closeResultSet() throws SQLException
	{
		if(resultSet!=null)
		{
			resultSet.close();
			resultSet=null;
		}
	}

	private void closePreparedStatement() throws SQLException
	{
		closeResultSet();

		if(preparedStatement!=null)
		{
			preparedStatement.close();
			preparedStatement=null;
		}
	}

	private void closeStatement() throws SQLException
	{
		closeResultSet();

		if(statement!=null)
		{
			statement.close();
			statement=null;
		}
	}

	public void close()
	{
		try
		{
			closePreparedStatement();
			closeStatement();

			if(connection!=null)
			{
				connection.close();
				connection=null;
			}
		}
		catch(SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	public <T> List<T> fromQuery(Connection connection, Class<T> clazz, boolean ifClose, boolean ifOnceForAllData, String querySql, Object... objects) throws SQLException
	{
		setConnection(connection);
		return fromQuery(clazz, ifClose, ifOnceForAllData, querySql, objects);
	}

	public <T> List<T> fromQuery(Class<T> clazz, boolean ifClose, boolean ifOnceForAllData, String querySql, Object... objects) throws SQLException
	{
		executeQuery(ifOnceForAllData, querySql, objects);
		return fromResultSet(clazz, ifClose);
	}

	public <T> Iterable<T> queryIter(Connection connection, Class<T> clazz, boolean ifOnceForAllData, String querySql, Object... objects) throws SQLException
	{
		setConnection(connection);
		return queryIter(clazz, ifOnceForAllData, querySql, objects);
	}

	public <T> Iterable<T> queryIter(Class<T> clazz, boolean ifOnceForAllData, String querySql, Object... objects) throws SQLException
	{
		executeQuery(ifOnceForAllData, querySql, objects);
		return resultSetIter(clazz);
	}

	public <T> List<T> fromResultSet(Class<T> clazz, boolean ifClose)
	{
		try
		{
			return fromResultSet(resultSet, clazz);
		}
		finally
		{
			if(ifClose)
				close();
		}
	}

	public <T> List<T> fromResultSet(Consumer<String> loggerFunc, Class<T> clazz, boolean ifClose)
	{
		try
		{
			return fromResultSet(loggerFunc, resultSet, clazz);
		}
		finally
		{
			if(ifClose)
				close();
		}
	}

	public <T> Iterable<T> resultSetIter(Class<T> clazz)
	{
		return resultSetIter(resultSet, clazz);
	}

	public <T> Iterable<T> resultSetIter(Consumer<String> loggerFunc, Class<T> clazz)
	{
		return resultSetIter(loggerFunc, resultSet, clazz);
	}

	public static <T> List<T> fromResultSet(ResultSet resultSet, Class<T> clazz)
	{
		List<T> res=new ArrayList<T>();
		for(T obj : resultSetIter(resultSet, clazz))
		{
			res.add(obj);
		}
		return res;
	}

	public static <T> List<T> fromResultSet(Consumer<String> loggerFunc, ResultSet resultSet, Class<T> clazz)
	{
		List<T> res=new ArrayList<T>();
		for(T obj : resultSetIter(loggerFunc, resultSet, clazz))
		{
			res.add(obj);
		}
		return res;
	}

	public static <T> Iterable<T> resultSetIter(ResultSet resultSet, Class<T> clazz)
	{
		return resultSetIter(StdLoggers.STD_ERR_MSG_LOGGER, resultSet, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> Iterable<T> resultSetIter(Consumer<String> loggerFunc, ResultSet resultSet, Class<T> clazz)
	{
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator()
			{
				return new Iterator<T>() {
					private boolean first=true;
					private boolean directlyAssignable=false;

					private Map<Integer, Entry<Field, Method>> assignMethodsMap;
					private Field[] fields;
					private int[] fieldIdx;
					private Method[] methods;
					private int[] methodIdx;

					@Override
					public boolean hasNext()
					{
						try
						{
							return resultSet.next();
						}
						catch(SQLException e)
						{
							throw new RuntimeException(e);
						}
					}

					@Override
					public T next()
					{
						try
						{
							if(first)
							{
								first=false;
								ResultSetMetaData resultSetMetaData=resultSet.getMetaData();
								if(resultSetMetaData.getColumnCount()==1 && clazz.isAssignableFrom(resultSet.getObject(1).getClass()))
								{
									directlyAssignable=true;
									return (T)resultSet.getObject(1);
								}
								else
								{
									T obj=clazz.newInstance();
									Map<Integer, Field> fieldsMap=new HashMap<>();
									Map<Integer, Method> methodsMap=new HashMap<>();
									assignMethodsMap=new HashMap<>();
									Map<String, List<Method>> allMethodsMap=new HashMap<>();
									for(Method method : clazz.getMethods())
									{
										String name=method.getName();
										if(name.startsWith("set"))
										{
											if(!allMethodsMap.containsKey(name))
											{
												allMethodsMap.put(name, new ArrayList<Method>());
											}
											allMethodsMap.get(method.getName()).add(method);
										}
									}
									for(int i=1; i<=resultSetMetaData.getColumnCount(); i++)
									{
										String name=resultSetMetaData.getColumnName(i);
										Object fieldObject=resultSet.getObject(name);
										Field field=null;
										Method method=null;
										Method assignMethod=null;
										Exception exception=null;
										try
										{
											try
											{
												field=clazz.getField(name);
											}
											catch(Exception e)
											{
												exception=e;
												String name1=CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
												try
												{
													field=clazz.getField(name1);
												}
												catch(Exception e1)
												{
													e1.initCause(e);
													exception=e1;
													throw exception;
												}
											}

											try
											{
												field.set(obj, fieldObject);
												fieldsMap.put(i, field);
											}
											catch(Exception e)
											{
												if(exception!=null)
													e.initCause(exception);
												exception=e;
												Class<?> fieldClass=field.getType();
												if(Number.class.isAssignableFrom(fieldObject.getClass()) && (Number.class.isAssignableFrom(fieldClass) || fieldClass.isPrimitive()))
												{
													try
													{
														assignMethod=fieldObject.getClass().getMethod((fieldClass.isPrimitive() ? fieldClass.getSimpleName() : ((Class<? extends Number>)((Class<? extends Number>)fieldClass).getField("TYPE").get(null)).getSimpleName())+"Value");
														field.set(obj, assignMethod.invoke(fieldObject));
														assignMethodsMap.put(i, new AbstractMap.SimpleEntry<Field, Method>(field, assignMethod));
													}
													catch(Exception e1)
													{
														e1.initCause(e);
														exception=e1;
													}
												}
												if(assignMethod==null)
													throw exception;
											}
										}
										catch(Exception e)
										{
											List<Method> methodsList=allMethodsMap.get("set"+CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name));
											if(methodsList!=null)
											{
												for(Method m : methodsList)
												{
													try
													{
														m.invoke(obj, fieldObject);
														methodsMap.put(i, m);
														method=m;
														break;
													}
													catch(Exception e2)
													{}
												}
											}
											if(method==null)
											{
												if(field!=null)
													loggerFunc.accept(String.format("resultSetIter: Cannot set the field \"%s\" by database column \"%s\" with conjugate Java class \"%s\"", field.toString(), name, resultSetMetaData.getColumnClassName(i)));
												else
													loggerFunc.accept(String.format("resultSetIter: Cannot find a field for database column \"%s\" with conjugate Java class \"%s\"", name, resultSetMetaData.getColumnClassName(i)));
											}
										}
									}
									if(!fieldsMap.isEmpty() || !methodsMap.isEmpty() || !assignMethodsMap.isEmpty())
									{
										fields=new Field[fieldsMap.size()];
										fieldIdx=new int[fieldsMap.size()];
										methods=new Method[methodsMap.size()];
										methodIdx=new int[methodsMap.size()];
										int i=0;
										for(Entry<Integer, Field> entry : fieldsMap.entrySet())
										{
											fieldIdx[i]=entry.getKey();
											fields[i]=entry.getValue();
											++i;
										}
										i=0;
										for(Entry<Integer, Method> entry : methodsMap.entrySet())
										{
											methodIdx[i]=entry.getKey();
											methods[i]=entry.getValue();
											++i;
										}

										return obj;
									}
									else
										throw new IllegalArgumentException(String.format("resultSetIter::No field of \"%s\" can be set!", clazz.getName()));
								}
							}
							else
							{
								if(directlyAssignable)
									return (T)resultSet.getObject(1);
								else
								{
									T obj=clazz.newInstance();
									for(int i=0; i<fields.length; i++)
									{
										fields[i].set(obj, resultSet.getObject(fieldIdx[i]));
									}
									for(int i=0; i<methods.length; i++)
									{
										methods[i].invoke(obj, resultSet.getObject(methodIdx[i]));
									}
									for(Entry<Integer, Entry<Field, Method>> entry : assignMethodsMap.entrySet())
									{
										entry.getValue().getKey().set(obj, entry.getValue().getValue().invoke(resultSet.getObject(entry.getKey())));
									}
									return obj;
								}
							}
						}
						catch(Exception e)
						{
							throw new RuntimeException(e);
						}
					}
				};
			}
		};
	}
}
