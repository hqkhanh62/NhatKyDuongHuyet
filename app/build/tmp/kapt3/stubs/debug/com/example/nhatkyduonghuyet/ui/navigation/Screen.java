package com.example.nhatkyduonghuyet.ui.navigation;

import androidx.compose.runtime.Composable;
import androidx.compose.ui.Modifier;
import androidx.navigation.NavHostController;
import com.example.nhatkyduonghuyet.ml.GlucosePredictor;
import com.example.nhatkyduonghuyet.ml.GlucoseScanner;
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0007\u0007\b\t\n\u000b\f\rB\u000f\b\u0004\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u0082\u0001\u0007\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u00a8\u0006\u0015"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/navigation/Screen;", "", "route", "", "(Ljava/lang/String;)V", "getRoute", "()Ljava/lang/String;", "Chart", "Dashboard", "DateList", "DayDetail", "Prediction", "Scanner", "Search", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Chart;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Dashboard;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$DateList;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$DayDetail;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Prediction;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Scanner;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Search;", "app_debug"})
public abstract class Screen {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String route = null;
    
    private Screen(java.lang.String route) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRoute() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Chart;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen;", "()V", "app_debug"})
    public static final class Chart extends com.example.nhatkyduonghuyet.ui.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.nhatkyduonghuyet.ui.navigation.Screen.Chart INSTANCE = null;
        
        private Chart() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Dashboard;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen;", "()V", "app_debug"})
    public static final class Dashboard extends com.example.nhatkyduonghuyet.ui.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.nhatkyduonghuyet.ui.navigation.Screen.Dashboard INSTANCE = null;
        
        private Dashboard() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$DateList;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen;", "()V", "app_debug"})
    public static final class DateList extends com.example.nhatkyduonghuyet.ui.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.nhatkyduonghuyet.ui.navigation.Screen.DateList INSTANCE = null;
        
        private DateList() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$DayDetail;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen;", "()V", "app_debug"})
    public static final class DayDetail extends com.example.nhatkyduonghuyet.ui.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.nhatkyduonghuyet.ui.navigation.Screen.DayDetail INSTANCE = null;
        
        private DayDetail() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Prediction;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen;", "()V", "app_debug"})
    public static final class Prediction extends com.example.nhatkyduonghuyet.ui.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.nhatkyduonghuyet.ui.navigation.Screen.Prediction INSTANCE = null;
        
        private Prediction() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Scanner;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen;", "()V", "app_debug"})
    public static final class Scanner extends com.example.nhatkyduonghuyet.ui.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.nhatkyduonghuyet.ui.navigation.Screen.Scanner INSTANCE = null;
        
        private Scanner() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/nhatkyduonghuyet/ui/navigation/Screen$Search;", "Lcom/example/nhatkyduonghuyet/ui/navigation/Screen;", "()V", "app_debug"})
    public static final class Search extends com.example.nhatkyduonghuyet.ui.navigation.Screen {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.nhatkyduonghuyet.ui.navigation.Screen.Search INSTANCE = null;
        
        private Search() {
        }
    }
}