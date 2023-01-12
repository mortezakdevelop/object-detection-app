package com.example.objectdetectionapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.objectdetectionapp.R
import com.example.objectdetectionapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        fragmentHomeBinding = DataBindingUtil.inflate(layoutInflater,R.layout.fragment_home,container,false)
        return fragmentHomeBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        goToImageHelper()
    }

    private fun goToImageHelper(){
        fragmentHomeBinding.btnGoto.setOnClickListener{
            findNavController().navigate(R.id.action_homeFragment_to_imageHelperFragment)
        }
    }
}