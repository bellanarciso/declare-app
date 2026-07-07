package edu.illinois.cs.cs124.ay2026.project;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.data.CourseDataManager;
import edu.illinois.cs.cs124.ay2026.project.data.MajorRepository;
import edu.illinois.cs.cs124.ay2026.project.fragment.FinancialFragment;
import edu.illinois.cs.cs124.ay2026.project.fragment.PlansFragment;
import edu.illinois.cs.cs124.ay2026.project.fragment.ProfileFragment;
import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.Major;
import edu.illinois.cs.cs124.ay2026.project.model.RequirementGroup;

// Hosts the bottom navigation and its three fragments. Uses show/hide
// transactions instead of replace so each tab keeps its scroll position
// and loaded data when the user switches away and back.
public class HomeActivity extends AppCompatActivity {

    private Fragment plansFragment;
    private Fragment profileFragment;
    private Fragment financialFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        CourseDataManager.initialize(this);
        refreshCourseMetadata();

        if (savedInstanceState == null) {
            plansFragment     = new PlansFragment();
            profileFragment   = new ProfileFragment();
            financialFragment = new FinancialFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, financialFragment, FinancialFragment.TAG).hide(financialFragment)
                    .add(R.id.fragment_container, profileFragment,   ProfileFragment.TAG).hide(profileFragment)
                    .add(R.id.fragment_container, plansFragment,     PlansFragment.TAG)
                    .commit();
        } else {
            plansFragment     = getSupportFragmentManager().findFragmentByTag(PlansFragment.TAG);
            profileFragment   = getSupportFragmentManager().findFragmentByTag(ProfileFragment.TAG);
            financialFragment = getSupportFragmentManager().findFragmentByTag(FinancialFragment.TAG);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_plans)     { showFragment(plansFragment);     return true; }
            if (id == R.id.nav_profile)   { showFragment(profileFragment);   return true; }
            if (id == R.id.nav_financial) { showFragment(financialFragment); return true; }
            return false;
        });
    }

    // Kicks off a background refresh of course names and credit hours from the
    // CIS API. Gathers every course ID the app knows about, then invalidates the
    // repository cache when fresh data arrives so screens pick it up next load.
    private void refreshCourseMetadata() {
        Set<String> courseIds = new HashSet<>();
        List<Major> majors = MajorRepository.loadAll(this);
        for (Major major : majors) {
            for (Course course : major.getCourses()) {
                courseIds.add(course.getId());
            }
            for (RequirementGroup group : major.getRequirementGroups()) {
                for (Course opt : group.getOptions()) {
                    courseIds.add(opt.getId());
                }
            }
        }
        CourseDataManager.refreshIfNeeded(this, courseIds, MajorRepository::invalidateCache);
    }

    private void showFragment(Fragment target) {
        if (target == null) return;
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f != target) tx.hide(f);
        }
        tx.show(target);
        tx.commit();
    }
}